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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security 필터 체인에서
 * 403 (Forbidden) 오류가 발생했을 때(즉, 인가 실패 시)
 * ApiResult 형식의 JSON 응답을 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("JwtAccessDeniedHandler: 403 Forbidden. Message: {}", accessDeniedException.getMessage());

        // 403에 대한 오류 코드 (ErrorCode에 FORBIDDEN이 정의되어 있어야 함)
        ErrorCode errorCode = ErrorCode.FORBIDDEN; // "AUTH4004"
        String detailMessage = "해당 리소스에 접근할 권한이 없습니다.";

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