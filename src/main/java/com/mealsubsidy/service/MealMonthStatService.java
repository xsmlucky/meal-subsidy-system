package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.MealMonthStatQueryDTO;
import com.mealsubsidy.dto.MealMonthStatVO;
import com.mealsubsidy.entity.MealMonthStat;

public interface MealMonthStatService extends IService<MealMonthStat> {

    Page<MealMonthStatVO> pageStats(MealMonthStatQueryDTO query);

    MealMonthStatVO currentMonth();
}
