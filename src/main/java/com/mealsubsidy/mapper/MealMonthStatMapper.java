package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.MealMonthStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MealMonthStatMapper extends BaseMapper<MealMonthStat> {

    /**
     * 事务内行锁月度台账，防止并发订餐超扣。必须在 @Transactional 中调用。
     */
    @IgnoreDataPermission
    @Select("SELECT * FROM meal_month_stat "
            + "WHERE user_id = #{userId} AND stat_month = #{statMonth} AND is_deleted = 0 "
            + "LIMIT 1 FOR UPDATE")
    MealMonthStat selectByUserAndMonthForUpdate(@Param("userId") Long userId,
                                                @Param("statMonth") String statMonth);

    /**
     * 乐观锁冻结额度：WHERE 可用额度 >= amount，影响行数为 0 表示超扣或并发冲突。
     */
    @IgnoreDataPermission
    @Update("UPDATE meal_month_stat SET frozen_amount = frozen_amount + #{amount} "
            + "WHERE id = #{id} AND is_deleted = 0 "
            + "AND (quota_amount - frozen_amount - used_amount) >= #{amount}")
    int freezeAmountOptimistic(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 乐观锁释放冻结额度。
     */
    @IgnoreDataPermission
    @Update("UPDATE meal_month_stat SET frozen_amount = frozen_amount - #{amount} "
            + "WHERE id = #{id} AND is_deleted = 0 AND frozen_amount >= #{amount}")
    int unfreezeAmountOptimistic(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 乐观锁：冻结额度转为已使用额度（审核通过）。
     */
    @IgnoreDataPermission
    @Update("UPDATE meal_month_stat SET frozen_amount = frozen_amount - #{amount}, "
            + "used_amount = used_amount + #{amount} "
            + "WHERE id = #{id} AND is_deleted = 0 AND frozen_amount >= #{amount}")
    int confirmFrozenAmountOptimistic(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 乐观锁：退款冲回已使用额度。
     */
    @IgnoreDataPermission
    @Update("UPDATE meal_month_stat SET used_amount = used_amount - #{amount} "
            + "WHERE id = #{id} AND is_deleted = 0 AND used_amount >= #{amount}")
    int refundUsedAmountOptimistic(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
