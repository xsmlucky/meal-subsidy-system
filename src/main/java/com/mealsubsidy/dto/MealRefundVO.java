package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MealRefundVO {

    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    /** 申请人姓名 */
    private String userName;
    private Long deptId;
    private Long dishId;
    private String dishName;
    private LocalDate orderDate;
    private String mealType;
    private BigDecimal refundAmount;
    private String refundReason;
    /** REFUND_PENDING / REFUND_SUCCESS / REFUND_REJECT */
    private String refundStatus;
    private String orderStatus;
    private LocalDateTime createTime;
}
