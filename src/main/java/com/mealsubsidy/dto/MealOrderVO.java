package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MealOrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    /** 订餐人姓名 */
    private String userName;
    private Long deptId;
    private Long dishId;
    /** 菜品名称快照 */
    private String dishName;
    private LocalDate orderDate;
    private String mealType;
    /** 订单金额 */
    private BigDecimal orderAmount;
    /** PENDING / PASS / REJECT / CANCEL */
    private String orderStatus;
    private LocalDateTime createTime;
}
