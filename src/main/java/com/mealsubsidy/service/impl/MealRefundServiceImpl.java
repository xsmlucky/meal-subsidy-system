package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.common.util.BizNoUtil;
import com.mealsubsidy.dto.MealRefundQueryDTO;
import com.mealsubsidy.dto.MealRefundSubmitDTO;
import com.mealsubsidy.dto.MealRefundVO;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.entity.MealOrder;
import com.mealsubsidy.entity.MealRefund;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.mapper.MealOrderMapper;
import com.mealsubsidy.mapper.MealRefundMapper;
import com.mealsubsidy.mapper.SysUserMapper;
import com.mealsubsidy.service.MealRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealRefundServiceImpl extends ServiceImpl<MealRefundMapper, MealRefund> implements MealRefundService {

    public static final String REFUND_PENDING = "REFUND_PENDING";
    public static final String REFUND_SUCCESS = "REFUND_SUCCESS";
    public static final String REFUND_REJECT = "REFUND_REJECT";
    public static final String ORDER_REFUNDED = "REFUNDED";

    private final MealRefundMapper mealRefundMapper;
    private final MealOrderMapper mealOrderMapper;
    private final MealMonthStatMapper mealMonthStatMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Page<MealRefundVO> pageRefunds(MealRefundQueryDTO query) {
        AuthUser user = AuthGuard.requireLogin();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<MealRefund> wrapper = new LambdaQueryWrapper<>();
        if (user.isAdmin()) {
            wrapper.eq(MealRefund::getDeptId, user.getDeptId());
        } else {
            wrapper.eq(MealRefund::getUserId, user.getUserId());
        }
        wrapper.eq(StringUtils.hasText(query.getRefundStatus()), MealRefund::getRefundStatus, query.getRefundStatus());
        if (query.getOrderDate() != null) {
            List<Long> orderIds = mealOrderMapper.selectList(new LambdaQueryWrapper<MealOrder>()
                    .eq(MealOrder::getOrderDate, query.getOrderDate())
                    .select(MealOrder::getId))
                    .stream()
                    .map(MealOrder::getId)
                    .collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return new Page<>(current, size, 0);
            }
            wrapper.in(MealRefund::getOrderId, orderIds);
        }
        wrapper.orderByDesc(MealRefund::getId);
        Page<MealRefund> entityPage = mealRefundMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, MealOrder> orderMap = loadOrders(entityPage.getRecords());
        Map<Long, String> nameMap = loadUserNames(entityPage.getRecords());
        Page<MealRefundVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(item -> toVo(item, orderMap.get(item.getOrderId()), nameMap.get(item.getUserId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public MealRefundVO getRefund(Long id) {
        MealRefund refund = requireVisibleRefund(id);
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(refund.getOrderId());
        SysUser applicant = sysUserMapper.selectById(refund.getUserId());
        return toVo(refund, order, applicant == null ? null : applicant.getRealName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MealRefundVO submit(MealRefundSubmitDTO dto) {
        AuthUser user = AuthGuard.requireEmployee();
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(dto.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!user.getUserId().equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (!MealOrderServiceImpl.STATUS_PASS.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅已通过订单可申请退款");
        }
        if (order.getRefundDeadline() != null && LocalDateTime.now().isAfter(order.getRefundDeadline())) {
            throw new BizException(ResultCode.REFUND_DEADLINE_PASSED);
        }
        Long pendingCount = mealRefundMapper.selectCount(new LambdaQueryWrapper<MealRefund>()
                .eq(MealRefund::getOrderId, order.getId())
                .in(MealRefund::getRefundStatus, REFUND_PENDING, REFUND_SUCCESS));
        if (pendingCount != null && pendingCount > 0) {
            throw new BizException("该订单已有进行中或已成功的退款申请");
        }

        MealRefund refund = new MealRefund();
        refund.setRefundNo(BizNoUtil.refundNo());
        refund.setOrderId(order.getId());
        refund.setUserId(user.getUserId());
        refund.setDeptId(order.getDeptId());
        refund.setRefundAmount(order.getOrderAmount());
        refund.setRefundReason(dto.getRefundReason());
        refund.setRefundStatus(REFUND_PENDING);
        mealRefundMapper.insert(refund);
        return toVo(refund, order, user.getRealName());
    }

    /**
     * 同意退款：冲回已用额度，订单改为 REFUNDED。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealRefund refund = requireDeptRefund(id, admin);
        if (!REFUND_PENDING.equals(refund.getRefundStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核退款单可审核");
        }
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(refund.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!MealOrderServiceImpl.STATUS_PASS.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅已通过订单可退款");
        }
        MealMonthStat stat = requireMonthStat(order);
        int rows = mealMonthStatMapper.refundUsedAmountOptimistic(stat.getId(), refund.getRefundAmount());
        if (rows == 0) {
            throw new BizException(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }
        order.setOrderStatus(ORDER_REFUNDED);
        mealOrderMapper.updateById(order);
        refund.setRefundStatus(REFUND_SUCCESS);
        refund.setAuditorId(admin.getUserId());
        refund.setAuditOpinion("同意退款");
        refund.setAuditTime(LocalDateTime.now());
        mealRefundMapper.updateById(refund);
    }

    /**
     * 驳回退款：订单状态不变。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealRefund refund = requireDeptRefund(id, admin);
        if (!REFUND_PENDING.equals(refund.getRefundStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核退款单可审核");
        }
        refund.setRefundStatus(REFUND_REJECT);
        refund.setAuditorId(admin.getUserId());
        refund.setAuditOpinion("驳回退款申请");
        refund.setAuditTime(LocalDateTime.now());
        mealRefundMapper.updateById(refund);
    }

    private MealRefund requireVisibleRefund(Long id) {
        AuthUser user = AuthGuard.requireLogin();
        MealRefund refund = mealRefundMapper.selectByIdIgnorePermission(id);
        if (refund == null) {
            throw new BizException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        if (user.isAdmin()) {
            if (!user.getDeptId().equals(refund.getDeptId())) {
                throw new BizException(ResultCode.FORBIDDEN, "无权限查看其他部门退款单");
            }
        } else if (!user.getUserId().equals(refund.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return refund;
    }

    private MealRefund requireDeptRefund(Long id, AuthUser admin) {
        MealRefund refund = mealRefundMapper.selectByIdIgnorePermission(id);
        if (refund == null) {
            throw new BizException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        if (!admin.getDeptId().equals(refund.getDeptId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限审核其他部门退款单");
        }
        return refund;
    }

    private MealMonthStat requireMonthStat(MealOrder order) {
        String month = YearMonth.from(order.getOrderDate()).toString();
        MealMonthStat stat = mealMonthStatMapper.selectOne(new LambdaQueryWrapper<MealMonthStat>()
                .eq(MealMonthStat::getUserId, order.getUserId())
                .eq(MealMonthStat::getStatMonth, month)
                .last("LIMIT 1"));
        if (stat == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return stat;
    }

    private Map<Long, MealOrder> loadOrders(List<MealRefund> refunds) {
        Set<Long> ids = refunds.stream().map(MealRefund::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return mealOrderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MealOrder::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> loadUserNames(List<MealRefund> refunds) {
        Set<Long> ids = refunds.stream().map(MealRefund::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    private MealRefundVO toVo(MealRefund refund, MealOrder order, String userName) {
        MealRefundVO vo = new MealRefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setUserId(refund.getUserId());
        vo.setUserName(userName);
        vo.setDeptId(refund.getDeptId());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setRefundReason(refund.getRefundReason());
        vo.setRefundStatus(refund.getRefundStatus());
        vo.setCreateTime(refund.getCreateTime());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setDishId(order.getDishId());
            vo.setDishName(order.getDishName());
            vo.setOrderDate(order.getOrderDate());
            vo.setMealType(order.getMealType());
            vo.setOrderStatus(order.getOrderStatus());
        }
        return vo;
    }
}
