package com.mealsubsidy.dto;

import lombok.Data;

@Data
public class DeptQueryDTO {

    private Long current = 1L;

    private Long size = 10L;

    private String deptName;

    /** 1-启用 0-禁用 */
    private Integer status;
}
