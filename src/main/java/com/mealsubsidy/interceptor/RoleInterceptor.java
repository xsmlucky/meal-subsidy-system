package com.mealsubsidy.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealsubsidy.common.annotation.RequireRole;
import com.mealsubsidy.common.context.AuthContext;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null || requireRole.value().length == 0) {
            return true;
        }
        AuthUser user = AuthContext.get();
        if (user == null) {
            writeJson(response, Result.fail(ResultCode.UNAUTHORIZED));
            return false;
        }
        boolean allowed = Arrays.stream(requireRole.value()).anyMatch(role -> role.equals(user.getRole()));
        if (!allowed) {
            writeJson(response, Result.fail(ResultCode.FORBIDDEN));
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
