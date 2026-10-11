package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonthlyOrderRow {

    private String orderNo;
    private String realName;
    private LocalDate orderDate;
    private String mealType;
    private String dishName;
    private BigDecimal dishPrice;
    private BigDecimal orderAmount;
    private String orderStatus;
}
