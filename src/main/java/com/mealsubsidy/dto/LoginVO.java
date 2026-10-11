package com.mealsubsidy.dto;

import lombok.Data;

@Data
public class LoginVO {

    private String token;
    private Long userId;
    private Long deptId;
    private String username;
    private String realName;
    private String role;
}
