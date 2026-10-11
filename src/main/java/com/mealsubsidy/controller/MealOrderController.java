package com.mealsubsidy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.MealOrderQueryDTO;
import com.mealsubsidy.dto.MealOrderSubmitDTO;
import com.mealsubsidy.dto.MealOrderVO;
import com.mealsubsidy.service.MealOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/meal-order")
@RequiredArgsConstructor
public class MealOrderController {

    private final MealOrderService mealOrderService;

    @GetMapping("/page")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.EMPLOYEE})
    public Result<Page<MealOrderVO>> page(MealOrderQueryDTO query) {
        return Result.ok(mealOrderService.pageOrders(query));
    }

    @GetMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.EMPLOYEE})
    public Result<MealOrderVO> getById(@PathVariable("id") Long id) {
        return Result.ok(mealOrderService.getOrder(id));
    }

    @PostMapping("/submit")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<MealOrderVO> submit(@Valid @RequestBody MealOrderSubmitDTO dto) {
        return Result.ok(mealOrderService.submit(dto));
    }

    @PutMapping("/{id}/cancel")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<Void> cancel(@PathVariable("id") Long id) {
        mealOrderService.cancel(id);
        return Result.ok();
    }
}
