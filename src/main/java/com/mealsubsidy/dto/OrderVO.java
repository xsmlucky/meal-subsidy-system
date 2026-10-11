package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long deptId;
    private Long dishId;
    private String dishName;
    private BigDecimal dishPrice;
    private BigDecimal orderAmount;
    private LocalDate orderDate;
    private String mealType;
    private String orderStatus;
    private String rejectReason;
    private LocalDateTime refundDeadline;
    private LocalDateTime createTime;
}
