package com.pv.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Stateless JWT utility — chỉ VALIDATE token (token được issue bởi auth-service).
 * Dùng chung cho mọi service.
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey signingKey;

    public JwtUtils(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Parse và validate token. Trả về null nếu invalid/expired. */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
        }
        return null;
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}

