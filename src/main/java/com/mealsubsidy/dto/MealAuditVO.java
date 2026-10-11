package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MealAuditVO {

    /** 订单ID */
    private Long id;
    private String orderNo;
    private Long userId;
    /** 订餐人姓名 */
    private String userName;
    private Long deptId;
    /** 部门名称 */
    private String deptName;
    private Long dishId;
    private String dishName;
    private LocalDate orderDate;
    private String mealType;
    /** 冻结金额（订单金额） */
    private BigDecimal frozenAmount;
    private BigDecimal orderAmount;
    /** PENDING / PASS / REJECT / CANCEL */
    private String orderStatus;
    private LocalDateTime createTime;
}
