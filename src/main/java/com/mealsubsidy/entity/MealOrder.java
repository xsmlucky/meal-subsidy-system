package com.mealsubsidy.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订餐订单表 meal_order。
 * dish_name、dish_price 为下单快照，菜品改价改名不影响历史订单。
 */
@Data
@TableName("meal_order")
public class MealOrder {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，全局唯一 */
    private String orderNo;

    /** 下单员工ID，关联 sys_user.id */
    private Long userId;

    /** 下单时部门ID，管理员数据权限按此过滤 */
    private Long deptId;

    /** 菜品ID，关联 dish.id */
    private Long dishId;

    /** 菜品名称快照 */
    private String dishName;

    /** 菜品单价快照（元） */
    private BigDecimal dishPrice;

    /** 订单金额（元），等于菜品单价快照 */
    private BigDecimal orderAmount;

    /** 用餐日期 */
    private LocalDate orderDate;

    /** 餐次：BREAKFAST-早餐 LUNCH-午餐 DINNER-晚餐 */
    private String mealType;

    /**
     * 订单状态：PENDING-待审核 APPROVED-已通过 REJECTED-已驳回
     * CANCELLED-已撤销 REFUND_PENDING-退款待审 REFUNDED-已退款
     */
    private String orderStatus;

    /** 订单审核驳回原因 */
    private String rejectReason;

    /** 申请退款截止时间 */
    private LocalDateTime refundDeadline;

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
