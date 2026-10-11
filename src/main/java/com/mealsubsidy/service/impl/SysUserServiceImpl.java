package com.mealsubsidy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mealsubsidy.common.constant.RoleConstant;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.dto.UserQueryDTO;
import com.mealsubsidy.dto.UserSaveDTO;
import com.mealsubsidy.dto.UserVO;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.SysUserMapper;
import com.mealsubsidy.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final String DEFAULT_RAW_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> pageUsers(UserQueryDTO query) {
        Long deptId = currentAdminDeptId();
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : query.getSize();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, deptId)
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .eq(StringUtils.hasText(query.getRole()), SysUser::getRole, query.getRole())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getId);
        Page<SysUser> entityPage = sysUserMapper.selectPage(new Page<>(current, size), wrapper);
        Page<UserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public UserVO getUser(Long id) {
        return toVo(requireDeptUser(id));
    }

    @Override
    public void saveUser(UserSaveDTO dto) {
        validateRole(dto.getRole());
        if (sysUserMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setEmployeeNo(dto.getEmployeeNo());
        user.setDeptId(currentAdminDeptId());
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        validateStatus(user.getStatus());
        user.setPassword(passwordEncoder.encode(DEFAULT_RAW_PASSWORD));
        sysUserMapper.insert(user);
    }

    @Override
    public void updateUser(UserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("用户ID不能为空");
        }
        validateRole(dto.getRole());
        SysUser db = requireDeptUser(dto.getId());
        SysUser exists = sysUserMapper.selectByUsername(dto.getUsername());
        if (exists != null && !exists.getId().equals(dto.getId())) {
            throw new BizException("用户名已存在");
        }
        db.setUsername(dto.getUsername());
        db.setRealName(dto.getRealName());
        db.setEmployeeNo(dto.getEmployeeNo());
        db.setRole(dto.getRole());
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
            db.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            db.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        sysUserMapper.updateById(db);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        SysUser db = requireDeptUser(id);
        db.setStatus(status);
        sysUserMapper.updateById(db);
    }

    private Long currentAdminDeptId() {
        AuthUser admin = AuthGuard.requireAdmin();
        return admin.getDeptId();
    }

    private SysUser requireDeptUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!currentAdminDeptId().equals(user.getDeptId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限操作其他部门用户");
        }
        return user;
    }

    private void validateRole(String role) {
        if (!RoleConstant.ADMIN.equals(role) && !RoleConstant.EMPLOYEE.equals(role)) {
            throw new BizException("角色不合法");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态只能是 0-禁用 或 1-启用");
        }
    }

    private UserVO toVo(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
