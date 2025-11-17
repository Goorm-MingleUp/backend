package com.mingleup.backend.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mingleup.backend.global.exception.ErrorCode;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"success", "code", "message", "result"}) // "isSuccess" -> "success"로 변경
public class ApiResult<T> {

    private final boolean success; // "isSuccess" -> "success"로 변경
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    // 성공 시 생성자
    private ApiResult(boolean success, String code, String message, T result) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    // 실패 시 생성자 (T 타입 result)
    private ApiResult(boolean success, String code, String message, T result, boolean isFailure) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    // == 정적 팩토리 메서드 == //

    // 성공 (데이터 포함)
    public static <T> ApiResult<T> onSuccess(T data) {
        return new ApiResult<>(true, "COMMON200", "성공입니다.", data);
    }

    // 성공 (데이터 미포함, 예: CUD)
    public static <T> ApiResult<T> onSuccess() {
        return new ApiResult<>(true, "COMMON200", "성공입니다.", null);
    }

    /**
     * [수정] 실패 시 (result에 상세 메시지 포함)
     * @param errorCode
     * @param detailMessage (result 필드에 담길 상세 메시지)
     * @return
     */
    public static <T> ApiResult<T> onFailure(ErrorCode errorCode, T detailMessage) {
        return new ApiResult<>(false, errorCode.getCode(), errorCode.getMessage(), detailMessage, true);
    }

    /**
     * [수정] 실패 시 (result 없음)
     * @param errorCode
     * @return
     */
    public static <T> ApiResult<T> onFailure(ErrorCode errorCode) {
        return new ApiResult<>(false, errorCode.getCode(), errorCode.getMessage(), null, true);
    }

    // (참고: 기존 'onFailure' 오버로딩 중 일부는 명확성을 위해 위 2개로 통일합니다.)
}