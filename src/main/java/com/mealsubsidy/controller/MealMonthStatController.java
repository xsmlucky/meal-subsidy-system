package com.mealsubsidy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.MealMonthStatQueryDTO;
import com.mealsubsidy.dto.MealMonthStatVO;
import com.mealsubsidy.service.MealMonthStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-month-stat")
@RequiredArgsConstructor
public class MealMonthStatController {

    private final MealMonthStatService mealMonthStatService;

    @GetMapping("/page")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.EMPLOYEE})
    public Result<Page<MealMonthStatVO>> page(MealMonthStatQueryDTO query) {
        return Result.ok(mealMonthStatService.pageStats(query));
    }

    @GetMapping("/current")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<MealMonthStatVO> current() {
        return Result.ok(mealMonthStatService.currentMonth());
    }
}
