package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.MealRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MealRefundMapper extends BaseMapper<MealRefund> {

    @IgnoreDataPermission
    @Select("SELECT * FROM meal_refund WHERE id = #{id} AND is_deleted = 0")
    MealRefund selectByIdIgnorePermission(@Param("id") Long id);
}
