package com.mingleup.backend.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage; // [추가] result 필드에 담길 상세 메시지

    /**
     * [수정] 상세 메시지를 받지 않는 경우, ErrorCode의 메시지를 상세 메시지로 사용
     * @param errorCode
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = errorCode.getMessage(); // [수정]
    }

    /**
     * [추가] 상세 메시지를 받는 생성자
     * @param errorCode
     * @param detailMessage (ApiResult의 'result' 필드에 담길 메시지)
     */
    public CustomException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage()); // RuntimeException의 message는 ErrorCode의 기본 메시지를 사용
        this.errorCode = errorCode;
        this.detailMessage = detailMessage; // 'result'에 담길 상세 메시지는 따로 보관
    }
}

