package com.mealsubsidy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.MealRefundQueryDTO;
import com.mealsubsidy.dto.MealRefundSubmitDTO;
import com.mealsubsidy.dto.MealRefundVO;
import com.mealsubsidy.service.MealRefundService;
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
@RequestMapping("/api/meal-refund")
@RequiredArgsConstructor
public class MealRefundController {

    private final MealRefundService mealRefundService;

    @GetMapping("/page")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.EMPLOYEE})
    public Result<Page<MealRefundVO>> page(MealRefundQueryDTO query) {
        return Result.ok(mealRefundService.pageRefunds(query));
    }

    @GetMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.EMPLOYEE})
    public Result<MealRefundVO> getById(@PathVariable("id") Long id) {
        return Result.ok(mealRefundService.getRefund(id));
    }

    @PostMapping("/submit")
    @RequireRole(RoleConstant.EMPLOYEE)
    public Result<MealRefundVO> submit(@Valid @RequestBody MealRefundSubmitDTO dto) {
        return Result.ok(mealRefundService.submit(dto));
    }

    @PutMapping("/{id}/approve")
    @RequireRole(RoleConstant.ADMIN)
    public Result<Void> approve(@PathVariable("id") Long id) {
        mealRefundService.approve(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @RequireRole(RoleConstant.ADMIN)
    public Result<Void> reject(@PathVariable("id") Long id) {
        mealRefundService.reject(id);
        return Result.ok();
    }
}
