package com.mealsubsidy.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单审核记录表 meal_audit
 */
@Data
@TableName("meal_audit")
public class MealAudit {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID，关联 meal_order.id */
    private Long orderId;

    /** 订单所属部门ID */
    private Long deptId;

    /** 审核人用户ID，关联 sys_user.id */
    private Long auditorId;

    /** 审核结果：PASS-通过 REJECT-驳回 */
    private String auditResult;

    /** 审核意见，驳回时必填 */
    private String auditOpinion;

    /** 审核时间 */
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
