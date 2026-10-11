package com.mealsubsidy.common.util;

import com.mealsubsidy.common.context.AuthUser;
import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_DEPT_ID = "deptId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USERNAME = "username";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-seconds}")
    private long expireSeconds;

    public String createToken(AuthUser user) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_DEPT_ID, user.getDeptId())
                .claim(CLAIM_ROLE, user.getRole())
                .claim(CLAIM_USERNAME, user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthUser parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            AuthUser user = new AuthUser();
            user.setUserId(asLong(claims.get(CLAIM_USER_ID)));
            user.setDeptId(asLong(claims.get(CLAIM_DEPT_ID)));
            user.setRole(claims.get(CLAIM_ROLE, String.class));
            user.setUsername(claims.get(CLAIM_USERNAME, String.class));
            return user;
        } catch (ExpiredJwtException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED, "无效令牌");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
