package com.mingleup.backend.global.auth;

import com.mingleup.backend.domian.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * MingleUp 자체 JWT를 생성하고 검증하는 유틸리티
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationInMs;

    public JwtTokenProvider(
            @Value("${app.jwtSecret}") String secretKey,
            @Value("${app.jwtExpirationInMs}") long expirationInMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationInMs = expirationInMs;
    }

    /**
     * @param userId MingleUp 유저 ID
     * @param role MingleUp 유저 Role
     * @return 생성된 JWT 문자열
     */
    public String createToken(Long userId, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);

        Claims claims = Jwts.claims().setSubject(userId.toString());
        // claims.put("kakaoId", user.getKakaoId()); // kakaoId는 JWT에 필수가 아니므로 일단 제외
        claims.put("role", role.name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // --- JWT 검증 로직 (추후 JwtAuthenticationFilter에서 사용) ---
    // (지금 당장은 로그인 구현에 필요하지 않아 생략)
    /*
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    */
}