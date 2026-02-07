package com.solutio.api.global.config;

import com.solutio.api.global.auth.filter.TokenAuthenticationFilter;
import com.solutio.api.global.auth.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final TokenProvider tokenProvider;

    private static final String[] ALLOWED_ALL = {
            "/",
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/api/v1/login",
            "/test/**",
            "/actuator/health"
    };

    private static final String[] ALLOWED_ALL_API_ENDPOINTS_GET = {
        "/api/v1/recruitments",
    };

    private static final String[] ALLOWED_ALL_API_ENDPOINTS_POST = {
        "/api/v1/applicants",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // URL 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ALLOWED_ALL).permitAll()
                        .requestMatchers(HttpMethod.GET,ALLOWED_ALL_API_ENDPOINTS_GET).permitAll()
                        .requestMatchers(HttpMethod.POST,ALLOWED_ALL_API_ENDPOINTS_POST).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                    ROLE_SUPER > ROLE_NEST
                    ROLE_NEST > ROLE_STAFF
                    ROLE_STAFF > ROLE_USER
                    ROLE_USER > ROLE_GUEST
                """);
    }
}