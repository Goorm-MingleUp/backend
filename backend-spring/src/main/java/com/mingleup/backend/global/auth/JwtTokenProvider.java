package com.mingleup.backend.global.auth;

import com.mingleup.backend.domain.user.domain.Role;
import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    // [수정] final 필드로 변경
    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;

    // [수정] 생성자에서 JwtProperties를 주입받아 필드 초기화
    public JwtTokenProvider(JwtProperties jwtProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityInMilliseconds = jwtProperties.getExpirationInMs();
    }

    /**
     * MingleUp 자체 JWT 토큰 생성
     */
    public String createToken(Long userId, Role role) {
        Claims claims = Jwts.claims().setSubject(userId.toString());
        claims.put("role", role.name());

        Date now = new Date();
        // [수정] final 필드 사용
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS512) // [수정] final 필드 사용
                .compact();
    }

    /**
     * Request Header에서 토큰 값을 가져옵니다. "Authorization" : "Bearer {TOKEN}"
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰의 유효성 + 만료일자 확인
     */
    public boolean validateToken(String token) {
        try {
            // [수정] final 필드 사용
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰에서 Claims 정보를 추출합니다.
     */
    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey) // [수정] final 필드 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰에서 인증 정보(Authentication)를 생성합니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        return new UsernamePasswordAuthenticationToken(userId, "", authorities);
    }

    /**
     * JWT에서 userId 추출   *** 동현 ***
     */
    public Long getUserId(String token){
        try {
            Claims claims = getClaims(token);
            // subject 혹은 userId claim 둘 중 하나에서 가져올 수 있게 처리
            Object id = claims.get("userId") != null ? claims.get("userId") : claims.getSubject();
            return Long.valueOf(id.toString());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}