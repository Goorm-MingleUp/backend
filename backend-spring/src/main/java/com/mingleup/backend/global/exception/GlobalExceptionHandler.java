package com.mingleup.backend.global.exception;

import com.mingleup.backend.global.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException; // Spring Boot 3.2+

/**
 * 전역 예외를 처리하는 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResult<String>> handleCustomException(CustomException e) {
        log.error("handleCustomException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResult.onFailure(errorCode, e.getDetailMessage()));
    }

    /**
     * [추가] 정적 리소스(favicon 등)가 없을 때 발생하는 404 예외 처리
     * 이를 처리하지 않으면 500 Server Error로 둔갑하여 로그를 더럽힙니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiResult<String>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(404)
                .body(ApiResult.onFailure(ErrorCode.USER_NOT_FOUND, "리소스를 찾을 수 없습니다: " + e.getResourcePath()));
    }

    /**
     * 나머지 모든 예외 처리 (500 Server Error)
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResult<String>> handleException(Exception e) {
        log.error("unhandledException: {}", e.getMessage(), e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // [수정] 디버깅을 위해 실제 에러 메시지(e.toString())를 반환합니다.
        // 배포 시에는 보안을 위해 다시 숨기는 것이 좋습니다.
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResult.onFailure(errorCode, "서버 에러 발생: " + e.toString()));
    }
}