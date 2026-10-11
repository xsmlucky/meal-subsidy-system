package com.mealsubsidy.dto;

import lombok.Data;

@Data
public class DishQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    private String mealType;

    /** 1-上架 0-下架 */
    private Integer status;

    private String dishName;
}
