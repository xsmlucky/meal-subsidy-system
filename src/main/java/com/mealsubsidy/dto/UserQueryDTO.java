package com.mealsubsidy.dto;

import lombok.Data;

@Data
public class UserQueryDTO {

    /** MyBatis-Plus 分页当前页 */
    private Long current = 1L;

    /** MyBatis-Plus 分页每页条数 */
    private Long size = 10L;

    private String username;

    private String realName;

    private Long deptId;

    private String role;

    /** 状态：1-启用 0-禁用 */
    private Integer status;
}
