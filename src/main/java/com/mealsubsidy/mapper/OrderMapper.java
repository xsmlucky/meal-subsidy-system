package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.MealOrder;
import com.mealsubsidy.dto.MonthlyOrderRow;
import com.mealsubsidy.dto.MonthlySummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 订餐订单 Mapper。历史金额只读 dish_name / dish_price 快照，不 JOIN dish。
 */
@Mapper
public interface OrderMapper extends BaseMapper<MealOrder> {

    @IgnoreDataPermission
    @Select("SELECT * FROM meal_order WHERE id = #{id} AND is_deleted = 0")
    MealOrder selectByIdIgnorePermission(@Param("id") Long id);

    @IgnoreDataPermission
    @Select("SELECT COUNT(1) FROM meal_order "
            + "WHERE user_id = #{userId} AND order_date = #{orderDate} AND meal_type = #{mealType} "
            + "AND order_status IN ('PENDING', 'APPROVED', 'REFUND_PENDING') AND is_deleted = 0")
    int countActiveOrder(@Param("userId") Long userId,
                         @Param("orderDate") LocalDate orderDate,
                         @Param("mealType") String mealType);

    /**
     * 部门月报汇总。条件走 idx_dept_id_order_date_order_status 最左前缀 (dept_id, order_date)，避免全表扫描。
     * 金额一律用订单快照 order_amount，不关联套餐表。
     */
    @IgnoreDataPermission
    @Select("SELECT "
            + "COUNT(DISTINCT CASE WHEN order_status = 'APPROVED' THEN user_id END) AS userCount, "
            + "COUNT(CASE WHEN order_status = 'APPROVED' THEN 1 END) AS approvedCount, "
            + "IFNULL(SUM(CASE WHEN order_status = 'APPROVED' THEN order_amount END), 0) AS approvedAmount, "
            + "COUNT(CASE WHEN order_status = 'REFUNDED' THEN 1 END) AS refundCount, "
            + "IFNULL(SUM(CASE WHEN order_status = 'REFUNDED' THEN order_amount END), 0) AS refundAmount "
            + "FROM meal_order "
            + "WHERE dept_id = #{deptId} AND is_deleted = 0 "
            + "AND order_date >= #{startDate} AND order_date < #{endDate}")
    MonthlySummaryVO selectMonthlySummary(@Param("deptId") Long deptId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @IgnoreDataPermission
    @Select("SELECT o.order_no AS orderNo, u.real_name AS realName, o.order_date AS orderDate, "
            + "o.meal_type AS mealType, o.dish_name AS dishName, o.dish_price AS dishPrice, "
            + "o.order_amount AS orderAmount, o.order_status AS orderStatus "
            + "FROM meal_order o "
            + "LEFT JOIN sys_user u ON o.user_id = u.id "
            + "WHERE o.dept_id = #{deptId} AND o.is_deleted = 0 "
            + "AND o.order_date >= #{startDate} AND o.order_date < #{endDate} "
            + "ORDER BY o.order_date, o.id")
    List<MonthlyOrderRow> listMonthlyOrders(@Param("deptId") Long deptId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
