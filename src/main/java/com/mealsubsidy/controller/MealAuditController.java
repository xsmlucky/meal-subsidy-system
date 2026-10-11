package com.mealsubsidy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.MealAuditQueryDTO;
import com.mealsubsidy.dto.MealAuditVO;
import com.mealsubsidy.service.MealAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/meal-audit")
@RequiredArgsConstructor
@Validated
@RequireRole(RoleConstant.ADMIN)
public class MealAuditController {

    private final MealAuditService mealAuditService;

    @GetMapping("/page")
    public Result<Page<MealAuditVO>> page(MealAuditQueryDTO query) {
        return Result.ok(mealAuditService.pagePending(query));
    }

    @GetMapping("/{id}")
    public Result<MealAuditVO> getById(@PathVariable("id") @NotNull Long id) {
        return Result.ok(mealAuditService.getOrder(id));
    }

    @PutMapping("/{id}/pass")
    public Result<Void> pass(@PathVariable("id") @NotNull Long id) {
        mealAuditService.pass(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable("id") @NotNull Long id) {
        mealAuditService.reject(id);
        return Result.ok();
    }
}
