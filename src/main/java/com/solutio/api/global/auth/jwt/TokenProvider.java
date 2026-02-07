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
    private final JwtProperties jwtProperties;

    public String generateToken(String userId, Duration expiredAt, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiredAt.toMillis());
        return makeToken(now, expiry, userId, role);
    }

    private String makeToken(Date now, Date expiry, String userId, String role) {
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
        if (role.equals("MEMBER")) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }
        if (role.equals("NEST")) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_NEST"));
        }
        if (role.equals("STAFF")) {
            return Collections.singleton(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if (role.equals("SUPER")) {
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

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}