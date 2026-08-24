package com.solutio.api.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProvider {
    public static final String CATEGORY_KEY = "category";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(String userId, String role) {
        return generateAccessToken(userId, jwtProperties.getAccessTokenExpiration(), role);
    }

    public String generateAccessToken(String userId, Duration expiredAt, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiredAt.toMillis());
        return makeToken(now, expiry, userId, role, TokenCategory.ACCESS);
    }

    public String generateRefreshToken(String userId, String role) {
        return generateRefreshToken(userId, jwtProperties.getRefreshTokenExpiration(), role);
    }

    public String generateRefreshToken(String userId, Duration expiredAt, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiredAt.toMillis());
        return makeToken(now, expiry, userId, role, TokenCategory.REFRESH);
    }

    private String makeToken(Date now, Date expiry, String userId, String role, TokenCategory category) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        return Jwts.builder()
            .header()
            .type("JWT")
            .and()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiration(expiry)
            .subject(userId)
            .claim("userId", userId)
            .claim("role", role)
            .claim(CATEGORY_KEY, category.name())
            .signWith(key)
            .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        Set<SimpleGrantedAuthority> authorities = getRoles(role);

        return new UsernamePasswordAuthenticationToken(
            new org.springframework.security.core.userdetails.User(
                claims.getSubject(),
                "",
                authorities
            ), token, authorities
        );
    }

    public Set<SimpleGrantedAuthority> getRoles(String role) {
        if ("USER".equals(role)) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
        }
        if ("NEST".equals(role)) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_NEST"));
        }
        if ("STAFF".equals(role)) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if ("SUPER".equals(role)) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_SUPER"));
        }
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    public boolean validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", String.class);
    }

    public TokenCategory getCategory(String token) {
        Claims claims = getClaims(token);
        String category = claims.get(CATEGORY_KEY, String.class);
        if (category == null) {
            return null;
        }
        try {
            return TokenCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isAccessToken(String token) {
        return TokenCategory.ACCESS.equals(getCategory(token));
    }

    public boolean isRefreshToken(String token) {
        return TokenCategory.REFRESH.equals(getCategory(token));
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}