package com.mingleup.backend.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingleup.backend.global.common.ApiResult;
import com.mingleup.backend.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security 필터 체인에서
 * 401 (Unauthorized) 오류가 발생했을 때(즉, 인증 실패 시)
 * ApiResult 형식의 JSON 응답을 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.warn("JwtAuthenticationEntryPoint: 401 Unauthorized. Message: {}", authException.getMessage());

        // 사용자가 요청한 형식으로 응답
        ErrorCode errorCode = ErrorCode.INVALID_TOKEN; // "AUTH4001"
        String detailMessage = "유효한 인증 정보가 없습니다.";

        sendErrorResponse(response, errorCode, detailMessage);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String detailMessage) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResult<String> errorResponse = ApiResult.onFailure(errorCode, detailMessage);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}