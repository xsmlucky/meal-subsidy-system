package com.mealsubsidy.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 使用 Spring BCrypt 生成密码哈希。可在 IDEA 中直接运行 main。
 */
public final class BCryptHashGenerator {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private BCryptHashGenerator() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        System.out.println(encode(raw));
    }
}
