package com.mingleup.backend.global.auth;

import com.mingleup.backend.global.exception.CustomException;
import com.mingleup.backend.global.exception.ErrorCode;
import com.mingleup.backend.global.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        String requestURI = request.getRequestURI();

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("인증 정보 저장: {}, uri: {}", authentication.getName(), requestURI);
            }
        } catch (CustomException e) {
            // [핵심 수정] 토큰이 유효하지 않아도(만료 등) 에러를 즉시 반환하지 않습니다.
            // permitAll 경로(로그인 등)는 통과해야 하기 때문입니다.
            // 대신 SecurityContext에 인증 정보가 없으므로, 인증이 필요한 페이지는 알아서 401이 뜹니다.
            log.debug("유효하지 않은 토큰입니다. uri: {}, cause: {}", requestURI, e.getMessage());
        }

        // 어떤 경우든 다음 필터로 진행합니다.
        filterChain.doFilter(request, response);
    }

    /**
     * Request Header 또는 Cookie에서 토큰 값을 가져옵니다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "accessToken".equals(c.getName()))
                    .findFirst()
                    .map(cookie -> cookie.getValue())
                    .orElse(null);
        }
        return null;
    }

    // sendErrorResponse 메서드는 더 이상 내부에서 쓰이지 않지만, 유틸성으로 남겨둡니다.
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getStatus().value());
        ErrorResponse errorResponse = new ErrorResponse(errorCode);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}