package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.MealOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MealOrderMapper extends BaseMapper<MealOrder> {

    @IgnoreDataPermission
    @Select("SELECT * FROM meal_order WHERE id = #{id} AND is_deleted = 0")
    MealOrder selectByIdIgnorePermission(@Param("id") Long id);
}
