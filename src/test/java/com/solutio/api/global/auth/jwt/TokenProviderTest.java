package com.solutio.api.global.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    private TokenProvider tokenProvider;
    private JwtProperties jwtProperties;
    private static final String SECRET_KEY = "c29sdXRpby1zZWNyZXQta2V5LXNvbHV0aW8tc2VjcmV0LWtleS1zb2x1dGlvLXNlY3JldC1rZXk=";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("solutio-test");
        jwtProperties.setSecretKey(SECRET_KEY);
        tokenProvider = new TokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("generateAccessToken으로 생성된 토큰은 category가 ACCESS이고 유효한 토큰이다")
    void generateAccessToken_createsValidAccessToken() {
        String userId = "202612345";
        String role = "USER";

        String token = tokenProvider.generateAccessToken(userId, role);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getCategory(token)).isEqualTo(TokenCategory.ACCESS);
        assertThat(tokenProvider.isAccessToken(token)).isTrue();
        assertThat(tokenProvider.isRefreshToken(token)).isFalse();
        assertThat(tokenProvider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("generateRefreshToken으로 생성된 토큰은 category가 REFRESH이고 유효한 토큰이다")
    void generateRefreshToken_createsValidRefreshToken() {
        String userId = "202612345";
        String role = "USER";

        String token = tokenProvider.generateRefreshToken(userId, role);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getCategory(token)).isEqualTo(TokenCategory.REFRESH);
        assertThat(tokenProvider.isRefreshToken(token)).isTrue();
        assertThat(tokenProvider.isAccessToken(token)).isFalse();
        assertThat(tokenProvider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Duration을 지정하여 generateAccessToken과 generateRefreshToken을 호출할 수 있다")
    void generateToken_withCustomDuration() {
        String userId = "202612345";
        String role = "STAFF";

        String accessToken = tokenProvider.generateAccessToken(userId, Duration.ofMinutes(30), role);
        String refreshToken = tokenProvider.generateRefreshToken(userId, Duration.ofHours(12), role);

        assertThat(tokenProvider.isAccessToken(accessToken)).isTrue();
        assertThat(tokenProvider.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("getAuthentication은 토큰의 subject와 role 기반으로 Authentication 객체를 생성한다")
    void getAuthentication_createsValidAuthentication() {
        String userId = "202612345";
        String role = "NEST";
        String token = tokenProvider.generateAccessToken(userId, role);

        Authentication auth = tokenProvider.getAuthentication(token);

        assertThat(auth.getName()).isEqualTo(userId);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_NEST");
    }

    @Test
    @DisplayName("getRoles는 role이 null이거나 알 수 없는 값이더라도 NPE 없이 ROLE_GUEST를 반환한다")
    void getRoles_handlesNullAndUnknownSafely() {
        Set<SimpleGrantedAuthority> userRole = tokenProvider.getRoles("USER");
        Set<SimpleGrantedAuthority> nestRole = tokenProvider.getRoles("NEST");
        Set<SimpleGrantedAuthority> staffRole = tokenProvider.getRoles("STAFF");
        Set<SimpleGrantedAuthority> superRole = tokenProvider.getRoles("SUPER");
        Set<SimpleGrantedAuthority> nullRole = tokenProvider.getRoles(null);
        Set<SimpleGrantedAuthority> unknownRole = tokenProvider.getRoles("UNKNOWN");

        assertThat(userRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
        assertThat(nestRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_NEST");
        assertThat(staffRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_STAFF");
        assertThat(superRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_SUPER");
        assertThat(nullRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_GUEST");
        assertThat(unknownRole).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_GUEST");
    }
}
