package com.solutio.api.global.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationService {

    public static final String REVOKED_TOKEN_PREFIX = "logout:";
    private final StringRedisTemplate redisTemplate;

    public void revoke(String jti, Duration remainingExpiration) {
        if (jti == null || remainingExpiration == null || remainingExpiration.isZero() || remainingExpiration.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(REVOKED_TOKEN_PREFIX + jti, "logout", remainingExpiration);
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_TOKEN_PREFIX + jti));
    }
}
