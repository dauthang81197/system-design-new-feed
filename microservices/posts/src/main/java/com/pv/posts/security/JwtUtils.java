package com.pv.posts.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.access-secret}")
    private String accessSecret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = accessSecret.getBytes(StandardCharsets.UTF_8);
        // Pad/truncate to 32 bytes (256-bit) for HS256
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
        this.signingKey = Keys.hmacShaKeyFor(paddedKey);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);
        String sub = claims.getSubject();
        return UUID.fromString(sub);
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Unsupported token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Malformed token: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[JWT] Token validation failed: {}", e.getMessage());
        }
        return false;
    }
}

