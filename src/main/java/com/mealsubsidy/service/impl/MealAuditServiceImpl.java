package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.dto.MealAuditQueryDTO;
import com.mealsubsidy.dto.MealAuditVO;
import com.mealsubsidy.entity.Dept;
import com.mealsubsidy.entity.MealAudit;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.entity.MealOrder;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.DeptMapper;
import com.mealsubsidy.mapper.MealAuditMapper;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.mapper.MealOrderMapper;
import com.mealsubsidy.mapper.SysUserMapper;
import com.mealsubsidy.service.MealAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealAuditServiceImpl extends ServiceImpl<MealAuditMapper, MealAudit> implements MealAuditService {

    private final MealAuditMapper mealAuditMapper;
    private final MealOrderMapper mealOrderMapper;
    private final MealMonthStatMapper mealMonthStatMapper;
    private final SysUserMapper sysUserMapper;
    private final DeptMapper deptMapper;

    @Override
    public Page<MealAuditVO> pagePending(MealAuditQueryDTO query) {
        AuthUser admin = AuthGuard.requireAdmin();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<MealOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MealOrder::getDeptId, admin.getDeptId())
                .eq(MealOrder::getOrderStatus, MealOrderServiceImpl.STATUS_PENDING)
                .eq(query.getOrderDate() != null, MealOrder::getOrderDate, query.getOrderDate())
                .eq(query.getUserId() != null, MealOrder::getUserId, query.getUserId())
                .orderByDesc(MealOrder::getId);
        Page<MealOrder> entityPage = mealOrderMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> userNames = loadUserNames(entityPage.getRecords());
        Map<Long, String> deptNames = loadDeptNames(entityPage.getRecords());
        Page<MealAuditVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(item -> toVo(item, userNames.get(item.getUserId()), deptNames.get(item.getDeptId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public MealAuditVO getOrder(Long id) {
        MealOrder order = requireDeptOrder(id);
        SysUser orderUser = sysUserMapper.selectById(order.getUserId());
        Dept dept = deptMapper.selectById(order.getDeptId());
        return toVo(order,
                orderUser == null ? null : orderUser.getRealName(),
                dept == null ? null : dept.getDeptName());
    }

    /**
     * 审核通过：冻结额度转为已使用，订单变为 PASS。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(Long id) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealOrder order = requirePendingDeptOrder(id);
        MealMonthStat stat = requireMonthStat(order);
        int rows = mealMonthStatMapper.confirmFrozenAmountOptimistic(stat.getId(), order.getOrderAmount());
        if (rows == 0) {
            throw new BizException(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }
        order.setOrderStatus(MealOrderServiceImpl.STATUS_PASS);
        mealOrderMapper.updateById(order);
        insertAudit(order, admin, MealOrderServiceImpl.STATUS_PASS, "审核通过");
    }

    /**
     * 审核驳回：释放冻结额度，订单变为 REJECT。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealOrder order = requirePendingDeptOrder(id);
        MealMonthStat stat = requireMonthStat(order);
        int rows = mealMonthStatMapper.unfreezeAmountOptimistic(stat.getId(), order.getOrderAmount());
        if (rows == 0) {
            throw new BizException(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }
        order.setOrderStatus(MealOrderServiceImpl.STATUS_REJECT);
        mealOrderMapper.updateById(order);
        insertAudit(order, admin, MealOrderServiceImpl.STATUS_REJECT, "审核驳回");
    }

    private MealOrder requirePendingDeptOrder(Long id) {
        MealOrder order = requireDeptOrder(id);
        if (!MealOrderServiceImpl.STATUS_PENDING.equals(order.getOrderStatus())) {
            throw new BizException(ResultCode.ILLEGAL_ORDER_STATUS, "仅待审核订单可审核");
        }
        return order;
    }

    private MealOrder requireDeptOrder(Long id) {
        AuthUser admin = AuthGuard.requireAdmin();
        MealOrder order = mealOrderMapper.selectByIdIgnorePermission(id);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!admin.getDeptId().equals(order.getDeptId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限审核其他部门订单");
        }
        return order;
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

    private void insertAudit(MealOrder order, AuthUser admin, String result, String opinion) {
        MealAudit audit = new MealAudit();
        audit.setOrderId(order.getId());
        audit.setDeptId(order.getDeptId());
        audit.setAuditorId(admin.getUserId());
        audit.setAuditResult(result);
        audit.setAuditOpinion(opinion);
        audit.setAuditTime(LocalDateTime.now());
        mealAuditMapper.insert(audit);
    }

    private Map<Long, String> loadUserNames(List<MealOrder> orders) {
        Set<Long> ids = orders.stream().map(MealOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    private Map<Long, String> loadDeptNames(List<MealOrder> orders) {
        Set<Long> ids = orders.stream().map(MealOrder::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return deptMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getDeptName, (a, b) -> a));
    }

    private MealAuditVO toVo(MealOrder order, String userName, String deptName) {
        MealAuditVO vo = new MealAuditVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUserName(userName);
        vo.setDeptId(order.getDeptId());
        vo.setDeptName(deptName);
        vo.setDishId(order.getDishId());
        vo.setDishName(order.getDishName());
        vo.setOrderDate(order.getOrderDate());
        vo.setMealType(order.getMealType());
        vo.setFrozenAmount(order.getOrderAmount());
        vo.setOrderAmount(order.getOrderAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
