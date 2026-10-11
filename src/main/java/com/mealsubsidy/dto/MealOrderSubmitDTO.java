package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class MealOrderSubmitDTO {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "用餐日期不能为空")
    private LocalDate orderDate;

    @NotBlank(message = "餐次不能为空")
    private String mealType;
}
