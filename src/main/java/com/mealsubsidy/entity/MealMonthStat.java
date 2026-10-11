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
 * 月度餐补台账 meal_month_stat（一人一月一行）。
 * 可用额度 = quota_amount - frozen_amount - used_amount。
 * 订餐时对本行 SELECT ... FOR UPDATE 悲观锁，防止超扣。
 */
@Data
@TableName("meal_month_stat")
public class MealMonthStat {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工用户ID，关联 sys_user.id */
    private Long userId;

    /** 部门ID，关联 dept.id */
    private Long deptId;

    /** 统计月份，格式 YYYY-MM */
    private String statMonth;

    /** 本月发放餐补额度（元） */
    private BigDecimal quotaAmount;

    /** 预占额度（元），待审核订单占用 */
    private BigDecimal frozenAmount;

    /** 已用额度（元），审核通过后实扣 */
    private BigDecimal usedAmount;

    /** 本月有效订餐份数（已通过未退款） */
    private Integer orderCount;

    /** 本月退款通过份数 */
    private Integer refundCount;

    /** 本月退款通过金额（元） */
    private BigDecimal refundAmount;

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

    /**
     * 可用额度，不入库。
     */
    public BigDecimal availableAmount() {
        return quotaAmount.subtract(frozenAmount).subtract(usedAmount);
    }
}
