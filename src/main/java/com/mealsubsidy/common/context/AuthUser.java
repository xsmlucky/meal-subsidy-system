package com.mealsubsidy.common.context;

import com.mealsubsidy.common.constant.RoleConstant;
import lombok.Data;

import java.io.Serializable;

@Data
public class AuthUser implements Serializable {

    private Long userId;
    private Long deptId;
    private String username;
    private String realName;
    private String role;

    public boolean isAdmin() {
        return RoleConstant.ADMIN.equals(role);
    }

    public boolean isEmployee() {
        return RoleConstant.EMPLOYEE.equals(role);
    }
}
