package com.mealsubsidy.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MealRefundQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    private LocalDate orderDate;

    private String refundStatus;
}
