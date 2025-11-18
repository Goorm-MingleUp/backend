package com.mingleup.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드를 정의하는 Enum
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (공통)
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON4001", "유효하지 않은 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5001", "서버 내부 오류가 발생했습니다."),

    // Auth (인증)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4001", "인증에 실패했습니다."), // [수정]
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4002", "인증에 실패했습니다."), // [수정]
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH4003", "인증에 실패했습니다."), // [수정]
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH4004", "인가에 실패했습니다."), // [추가]

    // (참고: message는 "인증에 실패했습니다."로 통일하고, result에 상세 내용을 담습니다)

    // User (사용자)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4004", "데이터를 찾을 수 없습니다."), // [수정]

    // Party (파티)
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY404", "파티 정보를 찾을 수 없습니다."),
    HOST_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "HOST4004", "호스트 질문을 찾을 수 없습니다."),

    // Application
    APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "APPLICATION409", "이미 이 파티에 신청했습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICATION404", "신청 내역을 찾을 수 없습니다."),
    APPLICATION_CANNOT_CANCEL_CLOSED_PARTY(HttpStatus.BAD_REQUEST, "APPLICATION4002", "종료된 파티는 취소할 수 없습니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
