package com.mealsubsidy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.dto.DishQueryDTO;
import com.mealsubsidy.dto.DishSaveDTO;
import com.mealsubsidy.dto.DishVO;
import com.mealsubsidy.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/dish")
@RequiredArgsConstructor
@Validated
@RequireRole(RoleConstant.ADMIN)
public class DishController {

    private final DishService dishService;

    @GetMapping("/page")
    public Result<Page<DishVO>> page(DishQueryDTO query) {
        return Result.ok(dishService.pageDishes(query));
    }

    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable("id") Long id) {
        return Result.ok(dishService.getDish(id));
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody DishSaveDTO dto) {
        dishService.saveDish(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody DishSaveDTO dto) {
        dishService.updateDish(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @RequestParam
                                     @NotNull(message = "状态不能为空")
                                     @Min(value = 0, message = "状态只能是 0 或 1")
                                     @Max(value = 1, message = "状态只能是 0 或 1")
                                     Integer status) {
        dishService.updateStatus(id, status);
        return Result.ok();
    }
}
