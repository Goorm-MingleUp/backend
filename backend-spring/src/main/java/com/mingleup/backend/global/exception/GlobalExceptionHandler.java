package com.mingleup.backend.global.exception;

import com.mingleup.backend.global.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외를 처리하는 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException을 처리합니다.
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResult<String>> handleCustomException(CustomException e) {
        log.error("handleCustomException: {}", e.getErrorCode().getMessage());

        ErrorCode errorCode = e.getErrorCode();

        // [수정] e.getMessage() 대신 e.getDetailMessage()를 result에 담습니다.
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResult.onFailure(errorCode, e.getDetailMessage()));
    }

    /**
     * 처리하지 못한 나머지 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResult<String>> handleException(Exception e) {
        log.error("unhandledException: {}", e.getMessage(), e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // [수정] e.getMessage() 대신, 사용자가 요청한 형식의 상세 메시지를 담습니다.
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResult.onFailure(errorCode, "서버에서 요청 처리 중 오류가 발생했습니다."));
    }
}