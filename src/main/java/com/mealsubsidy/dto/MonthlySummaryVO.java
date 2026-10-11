package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlySummaryVO {

    private String accountMonth;
    private Long deptId;
    private Long userCount;
    private Long approvedCount;
    private BigDecimal approvedAmount;
    private Long refundCount;
    private BigDecimal refundAmount;
}
