package com.mealsubsidy.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeptVO {

    private Long id;
    private String deptName;
    /** 1-启用 0-禁用 */
    private Integer status;
    private LocalDateTime createTime;
}
