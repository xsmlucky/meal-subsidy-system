package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeptSaveDTO {

    /** 编辑时必填，新增为空 */
    private Long id;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    /** 1-启用 0-禁用，新增默认 1 */
    private Integer status;
}
