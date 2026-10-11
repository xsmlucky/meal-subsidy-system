package com.mealsubsidy.common.util;

import com.mealsubsidy.common.context.AuthContext;
import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;

public final class AuthGuard {

    private AuthGuard() {
    }

    public static AuthUser requireLogin() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static AuthUser requireAdmin() {
        AuthUser user = requireLogin();
        if (!user.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "需要管理员角色");
        }
        return user;
    }

    public static AuthUser requireEmployee() {
        AuthUser user = requireLogin();
        if (!user.isEmployee()) {
            throw new BizException(ResultCode.FORBIDDEN, "需要员工角色");
        }
        return user;
    }
}
