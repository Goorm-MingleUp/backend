package com.mingleup.backend.global.exception;

import lombok.Getter;

/**
 * 전역에서 사용할 커스텀 예외 클래스
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}