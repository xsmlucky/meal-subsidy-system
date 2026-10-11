package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.dto.MealMonthStatQueryDTO;
import com.mealsubsidy.dto.MealMonthStatVO;
import com.mealsubsidy.entity.MealMonthStat;
import com.mealsubsidy.mapper.MealMonthStatMapper;
import com.mealsubsidy.service.MealMonthStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealMonthStatServiceImpl extends ServiceImpl<MealMonthStatMapper, MealMonthStat>
        implements MealMonthStatService {

    private final MealMonthStatMapper mealMonthStatMapper;

    @Override
    public Page<MealMonthStatVO> pageStats(MealMonthStatQueryDTO query) {
        AuthUser user = AuthGuard.requireLogin();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<MealMonthStat> wrapper = new LambdaQueryWrapper<>();
        if (user.isAdmin()) {
            wrapper.eq(MealMonthStat::getDeptId, user.getDeptId());
            wrapper.eq(query.getUserId() != null, MealMonthStat::getUserId, query.getUserId());
        } else {
            wrapper.eq(MealMonthStat::getUserId, user.getUserId());
        }
        applyYearMonth(wrapper, query.getYear(), query.getMonth());
        wrapper.orderByDesc(MealMonthStat::getStatMonth).orderByDesc(MealMonthStat::getId);
        Page<MealMonthStat> entityPage = mealMonthStatMapper.selectPage(new Page<>(current, size), wrapper);
        Page<MealMonthStatVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public MealMonthStatVO currentMonth() {
        AuthUser user = AuthGuard.requireEmployee();
        String month = YearMonth.now().toString();
        MealMonthStat stat = mealMonthStatMapper.selectOne(new LambdaQueryWrapper<MealMonthStat>()
                .eq(MealMonthStat::getUserId, user.getUserId())
                .eq(MealMonthStat::getStatMonth, month)
                .last("LIMIT 1"));
        if (stat == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return toVo(stat);
    }

    private void applyYearMonth(LambdaQueryWrapper<MealMonthStat> wrapper, Integer year, Integer month) {
        if (year != null && month != null) {
            wrapper.eq(MealMonthStat::getStatMonth, formatStatMonth(year, month));
            return;
        }
        if (year != null) {
            wrapper.likeRight(MealMonthStat::getStatMonth, year + "-");
            return;
        }
        if (month != null) {
            wrapper.likeLeft(MealMonthStat::getStatMonth, "-" + padMonth(month));
        }
    }

    private String formatStatMonth(Integer year, Integer month) {
        return year + "-" + padMonth(month);
    }

    private String padMonth(Integer month) {
        if (month < 1 || month > 12) {
            throw new BizException(ResultCode.BAD_REQUEST, "月份必须在 1-12 之间");
        }
        return String.format("%02d", month);
    }

    private MealMonthStatVO toVo(MealMonthStat stat) {
        MealMonthStatVO vo = new MealMonthStatVO();
        vo.setId(stat.getId());
        vo.setUserId(stat.getUserId());
        vo.setDeptId(stat.getDeptId());
        YearMonth ym = parseStatMonth(stat.getStatMonth());
        if (ym != null) {
            vo.setYear(ym.getYear());
            vo.setMonth(ym.getMonthValue());
        }
        vo.setTotalAmount(stat.getQuotaAmount());
        vo.setUsedAmount(stat.getUsedAmount());
        vo.setFrozenAmount(stat.getFrozenAmount());
        vo.setAvailableAmount(stat.availableAmount());
        vo.setCreateTime(stat.getCreateTime());
        return vo;
    }

    private YearMonth parseStatMonth(String statMonth) {
        if (!StringUtils.hasText(statMonth)) {
            return null;
        }
        return YearMonth.parse(statMonth);
    }
}
