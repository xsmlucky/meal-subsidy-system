package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@IgnoreDataPermission
public interface DeptMapper extends BaseMapper<Dept> {
}
