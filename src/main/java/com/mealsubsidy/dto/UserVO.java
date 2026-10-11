package com.mealsubsidy.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String employeeNo;
    private Long deptId;
    private String role;
    /** 状态：1-启用 0-禁用 */
    private Integer status;
    private LocalDateTime createTime;
}
