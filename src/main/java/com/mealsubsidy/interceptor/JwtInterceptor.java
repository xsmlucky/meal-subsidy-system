package com.mealsubsidy.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealsubsidy.common.constant.CacheKey;
import com.mealsubsidy.common.context.AuthContext;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.common.result.ResultCode;
import com.mealsubsidy.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String headerValue = request.getHeader(header);
        if (!StringUtils.hasText(headerValue) || !headerValue.startsWith(prefix)) {
            writeJson(response, Result.fail(ResultCode.UNAUTHORIZED));
            return false;
        }
        String token = headerValue.substring(prefix.length()).trim();
        try {
            AuthUser user = jwtUtil.parseToken(token);
            AuthContext.set(mergeCache(user));
            return true;
        } catch (BizException ex) {
            writeJson(response, Result.fail(ex.getCode(), ex.getMessage()));
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private AuthUser mergeCache(AuthUser tokenUser) {
        try {
            Object cached = redisTemplate.opsForValue().get(CacheKey.LOGIN_USER + tokenUser.getUserId());
            if (cached instanceof AuthUser cachedUser) {
                return cachedUser;
            }
        } catch (Exception ex) {
            log.warn("读取登录缓存失败，回退 JWT 载荷: {}", ex.getMessage());
        }
        return tokenUser;
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
