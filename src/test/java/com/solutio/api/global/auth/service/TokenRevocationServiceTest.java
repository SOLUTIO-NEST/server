package com.solutio.api.global.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @InjectMocks
    private TokenRevocationService tokenRevocationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("유효한 jti와 잔여 만료 시간이 주어지면 Redis에 폐기(revoke) 토큰으로 등록된다")
    void revoke_validJtiAndExpiration_savesToRedis() {
        String jti = "test-jti-uuid";
        Duration remainingExpiration = Duration.ofMinutes(30);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        tokenRevocationService.revoke(jti, remainingExpiration);

        verify(valueOperations).set("logout:" + jti, "logout", remainingExpiration);
    }

    @Test
    @DisplayName("잔여 만료 시간이 0 이하이거나 null인 경우 Redis에 등록하지 않는다 (TTL 엣지 케이스)")
    void revoke_zeroOrNegativeExpiration_skipsRedisSave() {
        String jti = "test-jti-uuid";

        tokenRevocationService.revoke(jti, Duration.ZERO);
        tokenRevocationService.revoke(jti, Duration.ofSeconds(-10));
        tokenRevocationService.revoke(jti, null);
        tokenRevocationService.revoke(null, Duration.ofMinutes(10));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Redis에 키가 존재하면 isRevoked는 true를 반환한다")
    void isRevoked_whenKeyExists_returnsTrue() {
        String jti = "test-jti-uuid";
        given(redisTemplate.hasKey("logout:" + jti)).willReturn(true);

        boolean result = tokenRevocationService.isRevoked(jti);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis에 키가 존재하지 않으면 isRevoked는 false를 반환한다")
    void isRevoked_whenKeyDoesNotExist_returnsFalse() {
        String jti = "test-jti-uuid";
        given(redisTemplate.hasKey("logout:" + jti)).willReturn(false);

        boolean result = tokenRevocationService.isRevoked(jti);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("jti가 null이면 isRevoked는 false를 반환한다")
    void isRevoked_whenJtiIsNull_returnsFalse() {
        boolean result = tokenRevocationService.isRevoked(null);

        assertThat(result).isFalse();
        verify(redisTemplate, never()).hasKey(anyString());
    }
}
