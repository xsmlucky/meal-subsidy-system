package com.mealsubsidy.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MealAuditQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    private LocalDate orderDate;

    private Long userId;
}
