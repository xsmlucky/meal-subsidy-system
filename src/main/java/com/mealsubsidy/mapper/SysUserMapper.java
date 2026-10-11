package com.mealsubsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealsubsidy.common.annotation.IgnoreDataPermission;
import com.mealsubsidy.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @IgnoreDataPermission
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);
}
