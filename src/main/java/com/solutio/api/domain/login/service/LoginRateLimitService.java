package com.solutio.api.domain.login.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitService {

    public static final String PAIR_KEY_PREFIX = "login:fail:pair:";
    public static final String ID_KEY_PREFIX = "login:fail:id:";
    public static final String IP_KEY_PREFIX = "login:fail:ip:";

    private static final String RECORD_FAILURE_LUA = """
        local pair = redis.call('INCR', KEYS[1])
        redis.call('EXPIRE', KEYS[1], ARGV[1])
        local id = redis.call('INCR', KEYS[2])
        redis.call('EXPIRE', KEYS[2], ARGV[2])
        local ip = redis.call('INCR', KEYS[3])
        redis.call('EXPIRE', KEYS[3], ARGV[3])
        return {pair, id, ip}
        """;

    // 결과 목록은 미사용이므로 원시 타입 List로 선언 (List.class -> MULTI 매핑)
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(RECORD_FAILURE_LUA, List.class);

    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitProperties properties;

    public boolean isBlocked(String userId, String clientIp) {
        try {
            List<String> counts = redisTemplate.opsForValue().multiGet(keys(userId, clientIp));
            return counts != null
                && (countAtLeast(counts.get(0), properties.getPair().getMaxFailures())
                || countAtLeast(counts.get(1), properties.getId().getMaxFailures())
                || countAtLeast(counts.get(2), properties.getIp().getMaxFailures()));
        } catch (Exception e) {
            log.warn("로그인 Rate Limit 카운터 조회 실패 - Fail-Open 처리 (userId={}, ip={})", userId, clientIp, e);
            return false;
        }
    }

    public void recordFailure(String userId, String clientIp) {
        try {
            List<String> keyList = keys(userId, clientIp);
            redisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                keyList,
                String.valueOf(properties.getPair().getBlockDuration().toSeconds()),
                String.valueOf(properties.getId().getBlockDuration().toSeconds()),
                String.valueOf(properties.getIp().getBlockDuration().toSeconds())
            );
        } catch (Exception e) {
            log.warn("로그인 실패 카운터 기록 실패 - Fail-Open 처리 (userId={}, ip={})", userId, clientIp, e);
        }
    }

    public void resetFailures(String userId, String clientIp) {
        try {
            redisTemplate.delete(keys(userId, clientIp));
        } catch (Exception e) {
            log.warn("로그인 실패 카운터 초기화 실패 - Fail-Open 처리 (userId={}, ip={})", userId, clientIp, e);
        }
    }

    public String resolveClientIp(HttpServletRequest request) {
        String headerName = properties.getClientIpHeader();
        if (headerName != null && !headerName.isBlank()) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isBlank()) {
                return ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private List<String> keys(String userId, String clientIp) {
        return List.of(
            PAIR_KEY_PREFIX + userId + ":" + clientIp,
            ID_KEY_PREFIX + userId,
            IP_KEY_PREFIX + clientIp
        );
    }

    private boolean countAtLeast(String count, int threshold) {
        if (count == null) {
            return false;
        }
        try {
            return Long.parseLong(count) >= threshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
