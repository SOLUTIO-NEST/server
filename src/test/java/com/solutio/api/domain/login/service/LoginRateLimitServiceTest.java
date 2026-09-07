package com.solutio.api.domain.login.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitServiceTest {

    private static final String USER_ID = "202612345";
    private static final String CLIENT_IP = "198.51.100.10";
    private static final String PAIR_KEY = LoginRateLimitService.PAIR_KEY_PREFIX + USER_ID + ":" + CLIENT_IP;
    private static final String ID_KEY = LoginRateLimitService.ID_KEY_PREFIX + USER_ID;
    private static final String IP_KEY = LoginRateLimitService.IP_KEY_PREFIX + CLIENT_IP;
    private static final List<String> THREE_KEYS = List.of(PAIR_KEY, ID_KEY, IP_KEY);
    private static final List<String> TTL_ARGS = List.of("300", "900", "900");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final LoginRateLimitProperties properties = new LoginRateLimitProperties();
    private LoginRateLimitService loginRateLimitService;

    @BeforeEach
    void setUp() {
        loginRateLimitService = new LoginRateLimitService(redisTemplate, properties);
    }

    @Test
    @DisplayName("로그인 실패 시 3계층 키(pair/id/ip)에 대해 Lua 스크립트로 INCR+EXPIRE가 원자 실행된다")
    void recordFailure_executesLuaScriptWithThreeKeysAndTtls() {
        loginRateLimitService.recordFailure(USER_ID, CLIENT_IP);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(THREE_KEYS),
                eq(TTL_ARGS.get(0)), eq(TTL_ARGS.get(1)), eq(TTL_ARGS.get(2)));
    }

    @Test
    @DisplayName("실패가 반복될 때마다 스크립트가 다시 실행되어 TTL이 매번 갱신된다")
    void recordFailure_refreshesTtlOnEveryFailure() {
        loginRateLimitService.recordFailure(USER_ID, CLIENT_IP);
        loginRateLimitService.recordFailure(USER_ID, CLIENT_IP);

        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class),
                eq(THREE_KEYS),
                eq(TTL_ARGS.get(0)), eq(TTL_ARGS.get(1)), eq(TTL_ARGS.get(2)));
    }

    @Test
    @DisplayName("pair 카운터가 임계치(5회)에 도달하면 차단된다")
    void isBlocked_whenPairCounterReachesThreshold_returnsTrue() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList("5", "4", "4"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("id 카운터가 임계치(30회)에 도달하면 pair 임계치 미달이라도 차단된다")
    void isBlocked_whenIdCounterReachesThreshold_returnsTrue() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList("4", "30", "4"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("ip 카운터가 임계치(30회)에 도달하면 차단된다")
    void isBlocked_whenIpCounterReachesThreshold_returnsTrue() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList("4", "29", "30"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("모든 계층의 카운터가 임계치 미달이면 차단되지 않는다")
    void isBlocked_belowAllThresholds_returnsFalse() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList("4", "29", "29"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("카운터가 아직 존재하지 않으면(null) 차단되지 않는다")
    void isBlocked_whenCountersMissing_returnsFalse() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList(null, null, null));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("카운터 값이 숫자가 아니면 차단 판정에서 무시된다")
    void isBlocked_whenCounterNotNumeric_ignoresValue() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection())).willReturn(Arrays.asList("corrupted", "29", "29"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 시 3계층 키 전부 삭제된다")
    void resetFailures_deletesAllThreeKeys() {
        loginRateLimitService.resetFailures(USER_ID, CLIENT_IP);

        verify(redisTemplate).delete(THREE_KEYS);
    }

    @Test
    @DisplayName("Redis 조회 실패 시 Fail-Open으로 차단하지 않는다")
    void isBlocked_redisFailure_failsOpen() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyCollection()))
                .willThrow(new RedisConnectionFailureException("redis down"));

        boolean blocked = loginRateLimitService.isBlocked(USER_ID, CLIENT_IP);

        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("Redis 기록 실패 시 Fail-Open으로 예외를 전파하지 않는다")
    void recordFailure_redisFailure_failsOpen() {
        willThrow(new RedisConnectionFailureException("redis down"))
                .given(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any());

        assertThatCode(() -> loginRateLimitService.recordFailure(USER_ID, CLIENT_IP))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis 초기화 실패 시 Fail-Open으로 예외를 전파하지 않는다")
    void resetFailures_redisFailure_failsOpen() {
        willThrow(new RedisConnectionFailureException("redis down"))
                .given(redisTemplate).delete(anyCollection());

        assertThatCode(() -> loginRateLimitService.resetFailures(USER_ID, CLIENT_IP))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("설정된 클라이언트 IP 헤더가 있으면 해당 값을 우선 사용한다")
    void resolveClientIp_configuredHeaderPresent_usesHeaderValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.setRemoteAddr("10.0.0.1");

        String clientIp = loginRateLimitService.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("설정된 헤더가 없으면 getRemoteAddr()로 폴백한다")
    void resolveClientIp_headerAbsent_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        String clientIp = loginRateLimitService.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("헤더 값이 공백이면 getRemoteAddr()로 폴백한다")
    void resolveClientIp_blankHeaderValue_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "   ");
        request.setRemoteAddr("10.0.0.1");

        String clientIp = loginRateLimitService.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("client-ip-header 설정 변경 시 해당 헤더를 사용한다")
    void resolveClientIp_customConfiguredHeader_usesThatHeader() {
        properties.setClientIpHeader("X-Forwarded-For");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "198.51.100.9");
        request.setRemoteAddr("10.0.0.1");

        String clientIp = loginRateLimitService.resolveClientIp(request);

        assertThat(clientIp).isEqualTo("198.51.100.9");
    }

    @Test
    @DisplayName("임계치·차단 시간 기본값은 pair 5회/5분, id 30회/15분, ip 30회/15분이다")
    void properties_defaultValues() {
        assertThat(properties.getPair().getMaxFailures()).isEqualTo(5);
        assertThat(properties.getPair().getBlockDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getId().getMaxFailures()).isEqualTo(30);
        assertThat(properties.getId().getBlockDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getIp().getMaxFailures()).isEqualTo(30);
        assertThat(properties.getIp().getBlockDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getClientIpHeader()).isEqualTo("CF-Connecting-IP");
    }
}
