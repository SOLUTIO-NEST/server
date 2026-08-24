package com.solutio.api.global.auth.filter;

import com.solutio.api.global.auth.jwt.JwtProperties;
import com.solutio.api.global.auth.jwt.TokenProvider;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAuthenticationFilterTest {

    private TokenProvider tokenProvider;
    private TokenAuthenticationFilter tokenAuthenticationFilter;
    private static final String SECRET_KEY = "c29sdXRpby1zZWNyZXQta2V5LXNvbHV0aW8tc2VjcmV0LWtleS1zb2x1dGlvLXNlY3JldC1rZXk=";

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("solutio-test");
        jwtProperties.setSecretKey(SECRET_KEY);
        tokenProvider = new TokenProvider(jwtProperties);
        tokenAuthenticationFilter = new TokenAuthenticationFilter(tokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Access Token으로 요청 시 SecurityContext에 인증 정보가 설정된다")
    void doFilter_withValidAccessToken_setsAuthentication() throws ServletException, IOException {
        String accessToken = tokenProvider.generateAccessToken("202612345", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        tokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("202612345");
    }

    @Test
    @DisplayName("Refresh Token으로 API 요청 시 SecurityContext에 인증 정보가 설정되지 않는다 (Token Type Confusion 차단)")
    void doFilter_withRefreshToken_doesNotSetAuthentication() throws ServletException, IOException {
        String refreshToken = tokenProvider.generateRefreshToken("202612345", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        tokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 SecurityContext에 인증 정보가 설정되지 않는다")
    void doFilter_withoutAuthorizationHeader_doesNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        tokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 요청 시 SecurityContext에 인증 정보가 설정되지 않는다")
    void doFilter_withInvalidToken_doesNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        tokenAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
