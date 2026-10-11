package com.mealsubsidy.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品表 dish
 */
@Data
@TableName("dish")
public class Dish {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品名称 */
    private String dishName;

    /** 菜品单价（元） */
    private BigDecimal price;

    /** 适用餐次：BREAKFAST-早餐 LUNCH-午餐 DINNER-晚餐 */
    private String mealType;

    /** 上架状态：0-下架 1-上架 */
    private Integer status;

    /** 排序号，越小越靠前 */
    private Integer sortNo;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
