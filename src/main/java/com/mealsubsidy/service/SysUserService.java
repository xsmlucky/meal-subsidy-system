package com.mealsubsidy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mealsubsidy.dto.UserQueryDTO;
import com.mealsubsidy.dto.UserSaveDTO;
import com.mealsubsidy.dto.UserVO;
import com.mealsubsidy.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    Page<UserVO> pageUsers(UserQueryDTO query);

    UserVO getUser(Long id);

    void saveUser(UserSaveDTO dto);

    void updateUser(UserSaveDTO dto);

    void updateStatus(Long id, Integer status);
}
