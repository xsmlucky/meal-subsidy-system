package com.mealsubsidy.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MealOrderQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    private LocalDate orderDate;

    private String mealType;

    private String orderStatus;

    private Long dishId;
}
