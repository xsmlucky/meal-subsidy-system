package com.mealsubsidy.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DishSaveDTO {

    /** 编辑时必填，新增为空 */
    private Long id;

    @NotBlank(message = "菜品名称不能为空")
    private String dishName;

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "菜品价格不能为空")
    private BigDecimal dishPrice;

    /** 1-上架 0-下架，新增默认 1 */
    private Integer status;

    /** 前端传入无效，服务端以当前管理员部门为准 */
    private Long deptId;
}
