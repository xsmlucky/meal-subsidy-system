package com.mealsubsidy.service.impl;

import com.mealsubsidy.common.constant.CacheKey;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.AuthGuard;
import com.mealsubsidy.common.util.JwtUtil;
import com.mealsubsidy.dto.LoginRequest;
import com.mealsubsidy.dto.LoginVO;
import com.mealsubsidy.entity.SysUser;
import com.mealsubsidy.mapper.SysUserMapper;
import com.mealsubsidy.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.expire-seconds}")
    private long expireSeconds;

    @Override
    public LoginVO login(LoginRequest request) {
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已停用");
        }
        AuthUser authUser = toAuthUser(user);
        String token = jwtUtil.createToken(authUser);
        try {
            redisTemplate.opsForValue().set(
                    CacheKey.LOGIN_USER + user.getId(),
                    authUser,
                    expireSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception ex) {
            log.warn("写入登录缓存失败，仍签发 JWT: {}", ex.getMessage());
        }

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setDeptId(user.getDeptId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public AuthUser currentUser() {
        return AuthGuard.requireLogin();
    }

    @Override
    public void logout() {
        AuthUser user = AuthGuard.requireLogin();
        try {
            redisTemplate.delete(CacheKey.LOGIN_USER + user.getUserId());
        } catch (Exception ex) {
            log.warn("清除登录缓存失败: {}", ex.getMessage());
        }
    }

    private AuthUser toAuthUser(SysUser user) {
        AuthUser authUser = new AuthUser();
        authUser.setUserId(user.getId());
        authUser.setDeptId(user.getDeptId());
        authUser.setUsername(user.getUsername());
        authUser.setRealName(user.getRealName());
        authUser.setRole(user.getRole());
        return authUser;
    }
}
