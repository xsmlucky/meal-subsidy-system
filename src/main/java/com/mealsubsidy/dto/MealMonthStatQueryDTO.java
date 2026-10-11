package com.mealsubsidy.dto;

import lombok.Data;

@Data
public class MealMonthStatQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    /** 年份，可选 */
    private Integer year;

    /** 月份 1-12，可选 */
    private Integer month;

    private Long userId;
}
