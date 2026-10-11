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
 * 退款申请与审核表 meal_refund
 */
@Data
@TableName("meal_refund")
public class MealRefund {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号，全局唯一 */
    private String refundNo;

    /** 原订单ID，关联 meal_order.id */
    private Long orderId;

    /** 申请员工ID，关联 sys_user.id */
    private Long userId;

    /** 部门ID，关联 dept.id */
    private Long deptId;

    /** 退款金额（元），取订单快照金额 */
    private BigDecimal refundAmount;

    /** 退款申请原因 */
    private String refundReason;

    /** 退款状态：PENDING-待审核 APPROVED-通过 REJECTED-驳回 */
    private String refundStatus;

    /** 退款审核人ID */
    private Long auditorId;

    /** 退款审核意见 */
    private String auditOpinion;

    /** 退款审核时间 */
    private LocalDateTime auditTime;

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
