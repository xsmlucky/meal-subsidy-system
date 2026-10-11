package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MealMonthStatVO {

    private Long id;
    private Long userId;
    private Long deptId;
    private Integer year;
    private Integer month;
    /** 本月发放额度，对应 quotaAmount */
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal frozenAmount;
    /** 可用额度 = totalAmount - frozenAmount - usedAmount */
    private BigDecimal availableAmount;
    private LocalDateTime createTime;
}
