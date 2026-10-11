package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealsubsidy.common.constant.MealType;
import com.mealsubsidy.common.constant.OrderStatus;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.common.util.BizNoUtil;
import com.mealsubsidy.dto.AuditOrderRequest;
import com.mealsubsidy.dto.MonthlyReportVO;
import com.mealsubsidy.dto.MonthlySummaryVO;
import com.mealsubsidy.dto.OrderVO;
import com.mealsubsidy.dto.RefundApplyRequest;
import com.mealsubsidy.dto.SubmitOrderRequest;
import com.mealsubsidy.entity.Dish;
import com.mealsubsidy.entity.MealAudit;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.entity.MealOrder;
import com.mealsubsidy.entity.MealRefund;
import com.mealsubsidy.mapper.DishMapper;
import com.mealsubsidy.mapper.MealAuditMapper;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.mapper.MealRefundMapper;
import com.mealsubsidy.mapper.OrderMapper;
import com.mealsubsidy.service.OrderService;
import com.mealsubsidy.service.excel.MonthlyReportExcelWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String PASS = "PASS";
    private static final String REJECT = "REJECT";
    private static final String REFUND_APPROVED = "APPROVED";
    private static final String REFUND_REJECTED = "REJECTED";
    private static final String REFUND_PENDING = "PENDING";

    private final OrderMapper orderMapper;
    private final DishMapper dishMapper;
    private final MealMonthStatMapper monthStatMapper;
    private final MealAuditMapper mealAuditMapper;
    private final MealRefundMapper mealRefundMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO submit(SubmitOrderRequest request) {
        AuthUser user = AuthGuard.requireEmployee();
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BizException(ResultCode.PACKAGE_UNAVAILABLE, "菜品不存在或已下架");
        }
        String mealType = StringUtils.hasText(request.getMealType()) ? request.getMealType() : dish.getMealType();
        validateMealType(mealType);
        if (request.getOrderDate().isBefore(LocalDate.now())) {
            throw new BizException("不能预订过去的日期");
        }
        BigDecimal snapshotPrice = dish.getPrice();
        String month = YearMonth.from(request.getOrderDate()).toString();
        MealMonthStat stat = lockStat(user.getUserId(), month);
        assertStatAccess(user, stat);
        if (stat.availableAmount().compareTo(snapshotPrice) < 0) {
            throw new BizException(ResultCode.INSUFFICIENT_BALANCE);
        }
        if (orderMapper.countActiveOrder(user.getUserId(), request.getOrderDate(), mealType) > 0) {
            throw new BizException(ResultCode.DUPLICATE_MEAL_ORDER);
        }
        stat.setFrozenAmount(stat.getFrozenAmount().add(snapshotPrice));
        monthStatMapper.updateById(stat);

        MealOrder order = new MealOrder();
        order.setOrderNo(BizNoUtil.orderNo());
        order.setUserId(user.getUserId());
        order.setDeptId(user.getDeptId());
        order.setDishId(dish.getId());
        order.setDishName(dish.getDishName());
        order.setDishPrice(snapshotPrice);
        order.setOrderAmount(snapshotPrice);
        order.setOrderDate(request.getOrderDate());
        order.setMealType(mealType);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setRefundDeadline(resolveRefundDeadline(request.getOrderDate(), mealType));
        orderMapper.insert(order);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancel(Long orderId) {
        AuthUser user = AuthGuard.requireEmployee();
        MealOrder order = loadOrder(orderId);
        assertOwner(user, order);
        if (!OrderStatus.PENDING.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核订单可撤销");
        }
        MealMonthStat stat = lockStat(order.getUserId(), YearMonth.from(order.getOrderDate()).toString());
        stat.setFrozenAmount(stat.getFrozenAmount().subtract(order.getOrderAmount()));
        monthStatMapper.updateById(stat);
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO audit(Long orderId, AuditOrderRequest request) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealOrder order = loadOrder(orderId);
        assertDept(admin, order);
        if (!OrderStatus.PENDING.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核订单可审核");
        }
        MealMonthStat stat = lockStat(order.getUserId(), YearMonth.from(order.getOrderDate()).toString());
        assertStatAccess(admin, stat);
        BigDecimal amount = order.getOrderAmount();
        boolean passed = Boolean.TRUE.equals(request.getPassed());
        if (passed) {
            stat.setFrozenAmount(stat.getFrozenAmount().subtract(amount));
            stat.setUsedAmount(stat.getUsedAmount().add(amount));
            stat.setOrderCount(stat.getOrderCount() + 1);
            order.setOrderStatus(OrderStatus.APPROVED);
        } else {
            if (!StringUtils.hasText(request.getRejectReason())) {
                throw new BizException(ResultCode.REJECT_REASON_REQUIRED);
            }
            stat.setFrozenAmount(stat.getFrozenAmount().subtract(amount));
            order.setOrderStatus(OrderStatus.REJECTED);
            order.setRejectReason(request.getRejectReason());
        }
        monthStatMapper.updateById(stat);
        orderMapper.updateById(order);
        insertAudit(order, admin.getUserId(), passed ? PASS : REJECT, request.getRejectReason());
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO applyRefund(Long orderId, RefundApplyRequest request) {
        AuthUser user = AuthGuard.requireEmployee();
        MealOrder order = loadOrder(orderId);
        assertOwner(user, order);
        if (!OrderStatus.APPROVED.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅已通过订单可申请退款");
        }
        if (order.getRefundDeadline() != null && !LocalDateTime.now().isBefore(order.getRefundDeadline())) {
            throw new BizException(ResultCode.REFUND_DEADLINE_PASSED);
        }
        MealRefund refund = new MealRefund();
        refund.setRefundNo(BizNoUtil.refundNo());
        refund.setOrderId(order.getId());
        refund.setUserId(order.getUserId());
        refund.setDeptId(order.getDeptId());
        refund.setRefundAmount(order.getOrderAmount());
        refund.setRefundReason(request == null ? null : request.getRefundReason());
        refund.setRefundStatus(REFUND_PENDING);
        mealRefundMapper.insert(refund);
        order.setOrderStatus(OrderStatus.REFUND_PENDING);
        orderMapper.updateById(order);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO auditRefund(Long refundId, AuditOrderRequest request) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealRefund refund = mealRefundMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        if (!admin.getDeptId().equals(refund.getDeptId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED, "无权限审核其他部门退款");
        }
        if (!REFUND_PENDING.equals(refund.getRefundStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核退款单可审核");
        }
        MealOrder order = loadOrder(refund.getOrderId());
        MealMonthStat stat = lockStat(order.getUserId(), YearMonth.from(order.getOrderDate()).toString());
        assertStatAccess(admin, stat);
        boolean passed = Boolean.TRUE.equals(request.getPassed());
        if (passed) {
            BigDecimal amount = refund.getRefundAmount();
            stat.setUsedAmount(stat.getUsedAmount().subtract(amount));
            stat.setOrderCount(Math.max(0, stat.getOrderCount() - 1));
            stat.setRefundCount(stat.getRefundCount() + 1);
            stat.setRefundAmount(stat.getRefundAmount().add(amount));
            monthStatMapper.updateById(stat);
            refund.setRefundStatus(REFUND_APPROVED);
            order.setOrderStatus(OrderStatus.REFUNDED);
        } else {
            if (!StringUtils.hasText(request.getRejectReason())) {
                throw new BizException(ResultCode.REJECT_REASON_REQUIRED);
            }
            refund.setRefundStatus(REFUND_REJECTED);
            order.setOrderStatus(OrderStatus.APPROVED);
        }
        refund.setAuditorId(admin.getUserId());
        refund.setAuditOpinion(request.getRejectReason());
        refund.setAuditTime(LocalDateTime.now());
        mealRefundMapper.updateById(refund);
        orderMapper.updateById(order);
        return toVo(order);
    }

    @Override
    public OrderVO detail(Long orderId) {
        AuthUser user = AuthGuard.requireLogin();
        MealOrder order = loadOrder(orderId);
        if (user.isEmployee()) {
            assertOwner(user, order);
        } else {
            assertDept(user, order);
        }
        return toVo(order);
    }

    @Override
    public List<OrderVO> myOrders() {
        AuthUser user = AuthGuard.requireLogin();
        LambdaQueryWrapper<MealOrder> wrapper = new LambdaQueryWrapper<>();
        if (user.isEmployee()) {
            wrapper.eq(MealOrder::getUserId, user.getUserId());
        } else {
            wrapper.eq(MealOrder::getDeptId, user.getDeptId());
        }
        wrapper.orderByDesc(MealOrder::getId);
        return orderMapper.selectList(wrapper).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public MonthlyReportVO monthlyReport(String accountMonth) {
        AuthUser admin = AuthGuard.requireAdmin();
        YearMonth month = parseMonth(accountMonth);
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        MonthlySummaryVO summary = orderMapper.selectMonthlySummary(admin.getDeptId(), start, end);
        if (summary == null) {
            summary = new MonthlySummaryVO();
        }
        summary.setAccountMonth(month.toString());
        summary.setDeptId(admin.getDeptId());
        zeroIfNull(summary);
        MonthlyReportVO vo = new MonthlyReportVO();
        vo.setSummary(summary);
        vo.setDetails(orderMapper.listMonthlyOrders(admin.getDeptId(), start, end));
        return vo;
    }

    @Override
    public void exportMonthlyReport(String accountMonth, HttpServletResponse response) {
        MonthlyReportVO report = monthlyReport(accountMonth);
        try {
            String filename = URLEncoder.encode("部门月度对账-" + report.getSummary().getAccountMonth() + ".xlsx",
                    StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            MonthlyReportExcelWriter.write(report, response.getOutputStream());
        } catch (Exception ex) {
            throw new BizException("导出 Excel 失败");
        }
    }

    private void insertAudit(MealOrder order, Long auditorId, String result, String opinion) {
        MealAudit audit = new MealAudit();
        audit.setOrderId(order.getId());
        audit.setDeptId(order.getDeptId());
        audit.setAuditorId(auditorId);
        audit.setAuditResult(result);
        audit.setAuditOpinion(opinion);
        audit.setAuditTime(LocalDateTime.now());
        mealAuditMapper.insert(audit);
    }

    private MealMonthStat lockStat(Long userId, String statMonth) {
        MealMonthStat stat = monthStatMapper.selectByUserAndMonthForUpdate(userId, statMonth);
        if (stat == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND, "本月餐补台账不存在，请联系管理员发放");
        }
        return stat;
    }

    private MealOrder loadOrder(Long orderId) {
        MealOrder order = orderMapper.selectByIdIgnorePermission(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void assertOwner(AuthUser user, MealOrder order) {
        if (!user.getUserId().equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
    }

    private void assertDept(AuthUser user, MealOrder order) {
        if (!user.getDeptId().equals(order.getDeptId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED, "无权限操作其他部门订单");
        }
    }

    private void assertStatAccess(AuthUser user, MealMonthStat stat) {
        if (user.isEmployee() && !user.getUserId().equals(stat.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (user.isAdmin() && !user.getDeptId().equals(stat.getDeptId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED, "无权限操作其他部门餐补台账");
        }
    }

    private LocalDateTime resolveRefundDeadline(LocalDate orderDate, String mealType) {
        if (MealType.BREAKFAST.equals(mealType)) {
            return orderDate.atTime(7, 30);
        }
        if (MealType.DINNER.equals(mealType)) {
            return orderDate.atTime(16, 0);
        }
        return orderDate.atTime(10, 30);
    }

    private void validateMealType(String mealType) {
        if (!MealType.BREAKFAST.equals(mealType)
                && !MealType.LUNCH.equals(mealType)
                && !MealType.DINNER.equals(mealType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "餐次不合法");
        }
    }

    private YearMonth parseMonth(String accountMonth) {
        try {
            return YearMonth.parse(accountMonth);
        } catch (Exception ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "账期格式应为 YYYY-MM");
        }
    }

    private void zeroIfNull(MonthlySummaryVO summary) {
        if (summary.getUserCount() == null) {
            summary.setUserCount(0L);
        }
        if (summary.getApprovedCount() == null) {
            summary.setApprovedCount(0L);
        }
        if (summary.getApprovedAmount() == null) {
            summary.setApprovedAmount(BigDecimal.ZERO);
        }
        if (summary.getRefundCount() == null) {
            summary.setRefundCount(0L);
        }
        if (summary.getRefundAmount() == null) {
            summary.setRefundAmount(BigDecimal.ZERO);
        }
    }

    private OrderVO toVo(MealOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}
