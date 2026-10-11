package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class SubmitOrderRequest {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "用餐日期不能为空")
    private LocalDate orderDate;

    /** BREAKFAST / LUNCH / DINNER，不传则用菜品餐次 */
    private String mealType;
}
