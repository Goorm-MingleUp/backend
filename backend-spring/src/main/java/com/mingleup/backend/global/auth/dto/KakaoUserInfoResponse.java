package com.mingleup.backend.global.auth.dto; // 패키지 경로 수정

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * 카카오 사용자 정보 응답 DTO
 * [수정] 카카오 콘솔의 '필수 동의' 항목('name' 등)에 맞춘 버전
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시
public class KakaoUserInfoResponse {

    @JsonProperty("id")
    private String kakaoId;

    // 'properties' (nickname, profile_image)는 "사용 안 함"이므로 DTO에서 제외

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {

        @JsonProperty("name")
        private String name; // "필수 동의" 항목 (이름)

        @JsonProperty("email")
        private String email; // "선택 동의" 또는 "사용 안 함" (null일 수 있음)

        @JsonProperty("gender")
        private String gender; // "필수 동의"

        @JsonProperty("age_range")
        private String ageRange; // "필수 동의"

        @JsonProperty("birthday")
        private String birthday; // "필수 동의" (MMDD)

        @JsonProperty("birthyear")
        private String birthyear; // "필S_ 동의" (YYYY)
    }
}