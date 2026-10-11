package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.constant.MealType;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.common.util.BizNoUtil;
import com.mealsubsidy.dto.MealOrderQueryDTO;
import com.mealsubsidy.dto.MealOrderSubmitDTO;
import com.mealsubsidy.dto.MealOrderVO;
import com.mealsubsidy.entity.Dish;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.entity.MealOrder;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.DishMapper;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.mapper.MealOrderMapper;
import com.mealsubsidy.mapper.SysUserMapper;
import com.mealsubsidy.service.MealOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealOrderServiceImpl extends ServiceImpl<MealOrderMapper, MealOrder> implements MealOrderService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_REJECT = "REJECT";
    public static final String STATUS_CANCEL = "CANCEL";

    private final MealOrderMapper mealOrderMapper;
    private final DishMapper dishMapper;
    private final MealMonthStatMapper mealMonthStatMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Page<MealOrderVO> pageOrders(MealOrderQueryDTO query) {
        AuthUser user = AuthGuard.requireLogin();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<MealOrder> wrapper = new LambdaQueryWrapper<>();
        if (user.isAdmin()) {
            wrapper.eq(MealOrder::getDeptId, user.getDeptId());
        } else {
            wrapper.eq(MealOrder::getUserId, user.getUserId());
        }
        wrapper.eq(query.getOrderDate() != null, MealOrder::getOrderDate, query.getOrderDate())
                .eq(StringUtils.hasText(query.getMealType()), MealOrder::getMealType, query.getMealType())
                .eq(StringUtils.hasText(query.getOrderStatus()), MealOrder::getOrderStatus, query.getOrderStatus())
                .eq(query.getDishId() != null, MealOrder::getDishId, query.getDishId())
                .orderByDesc(MealOrder::getId);
        Page<MealOrder> entityPage = mealOrderMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> nameMap = loadUserNames(entityPage.getRecords());
        Page<MealOrderVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(item -> toVo(item, nameMap.get(item.getUserId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public MealOrderVO getOrder(Long id) {
        MealOrder order = requireVisibleOrder(id);
        SysUser orderUser = sysUserMapper.selectById(order.getUserId());
        return toVo(order, orderUser == null ? null : orderUser.getRealName());
    }

    /**
     * 提交订餐：同一事务内创建订单并乐观锁冻结月度餐补，失败整单回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MealOrderVO submit(MealOrderSubmitDTO dto) {
        AuthUser user = AuthGuard.requireEmployee();
        validateMealType(dto.getMealType());
        if (dto.getOrderDate().isBefore(LocalDate.now())) {
            throw new BizException("不能预订过去的日期");
        }
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BizException(ResultCode.PACKAGE_UNAVAILABLE, "菜品不存在或已下架");
        }
        BigDecimal amount = dish.getPrice();
        String month = YearMonth.from(dto.getOrderDate()).toString();
        MealMonthStat stat = mealMonthStatMapper.selectOne(new LambdaQueryWrapper<MealMonthStat>()
                .eq(MealMonthStat::getUserId, user.getUserId())
                .eq(MealMonthStat::getStatMonth, month)
                .last("LIMIT 1"));
        if (stat == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND, "本月餐补台账不存在，请联系管理员发放");
        }
        if (stat.availableAmount().compareTo(amount) < 0) {
            throw new BizException(ResultCode.INSUFFICIENT_BALANCE);
        }
        int rows = mealMonthStatMapper.freezeAmountOptimistic(stat.getId(), amount);
        if (rows == 0) {
            throw new BizException(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }

        MealOrder order = new MealOrder();
        order.setOrderNo(BizNoUtil.orderNo());
        order.setUserId(user.getUserId());
        order.setDeptId(user.getDeptId());
        order.setDishId(dish.getId());
        order.setDishName(dish.getDishName());
        order.setDishPrice(amount);
        order.setOrderAmount(amount);
        order.setOrderDate(dto.getOrderDate());
        order.setMealType(dto.getMealType());
        order.setOrderStatus(STATUS_PENDING);
        order.setRefundDeadline(dto.getOrderDate().atTime(10, 30));
        mealOrderMapper.insert(order);
        return toVo(order, user.getRealName());
    }

    /**
     * 仅待审核可取消，同一事务释放冻结额度。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        AuthUser user = AuthGuard.requireEmployee();
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!user.getUserId().equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (!STATUS_PENDING.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核订单可取消");
        }
        String month = YearMonth.from(order.getOrderDate()).toString();
        MealMonthStat stat = mealMonthStatMapper.selectOne(new LambdaQueryWrapper<MealMonthStat>()
                .eq(MealMonthStat::getUserId, order.getUserId())
                .eq(MealMonthStat::getStatMonth, month)
                .last("LIMIT 1"));
        if (stat == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        int rows = mealMonthStatMapper.unfreezeAmountOptimistic(stat.getId(), order.getOrderAmount());
        if (rows == 0) {
            throw new BizException(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }
        order.setOrderStatus(STATUS_CANCEL);
        mealOrderMapper.updateById(order);
    }

    private MealOrder requireVisibleOrder(Long id) {
        AuthUser user = AuthGuard.requireLogin();
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (user.isAdmin()) {
            if (!user.getDeptId().equals(order.getDeptId())) {
                throw new BizException(ResultCode.FORBIDDEN, "无权限查看其他部门订单");
            }
        } else if (!user.getUserId().equals(order.getUserId())) {
            throw new BizException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private Map<Long, String> loadUserNames(List<MealOrder> orders) {
        Set<Long> ids = orders.stream().map(MealOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    private void validateMealType(String mealType) {
        if (!MealType.BREAKFAST.equals(mealType)
                && !MealType.LUNCH.equals(mealType)
                && !MealType.DINNER.equals(mealType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "餐次不合法");
        }
    }

    private MealOrderVO toVo(MealOrder order, String userName) {
        MealOrderVO vo = new MealOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUserName(userName);
        vo.setDeptId(order.getDeptId());
        vo.setDishId(order.getDishId());
        vo.setDishName(order.getDishName());
        vo.setOrderDate(order.getOrderDate());
        vo.setMealType(order.getMealType());
        vo.setOrderAmount(order.getOrderAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
