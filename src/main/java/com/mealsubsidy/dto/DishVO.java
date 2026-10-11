package com.mealsubsidy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DishVO {

    private Long id;
    private String dishName;
    private String mealType;
    private BigDecimal dishPrice;
    /** 1-上架 0-下架 */
    private Integer status;
    private Long deptId;
    private LocalDateTime createTime;
}
