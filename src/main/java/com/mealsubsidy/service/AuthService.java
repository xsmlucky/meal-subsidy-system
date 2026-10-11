package com.mealsubsidy.service;

import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.dto.LoginRequest;
import com.mealsubsidy.dto.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest request);

    AuthUser currentUser();

    void logout();
}
