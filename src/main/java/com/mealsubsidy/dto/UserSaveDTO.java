package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserSaveDTO {

    /** 编辑时必填，新增为空 */
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String employeeNo;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    @NotBlank(message = "角色不能为空")
    private String role;

    /** 新增忽略，固定 123456；编辑为空字符串则不更新密码 */
    private String password;

    /** 状态：1-启用 0-禁用 */
    private Integer status;
}
