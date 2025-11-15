package com.mingleup.backend.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * 카카오 사용자 정보 응답 DTO
 * [수정] 'profile_image' (선택 동의) 항목 추가
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시
public class KakaoUserInfoResponse {

    @JsonProperty("id")
    private String kakaoId;

    // [추가] 'profile_image'를 받기 위한 'properties'
    @JsonProperty("properties")
    private KakaoProperties properties;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    // [추가] 'properties' 내부 클래스
    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProperties {
        @JsonProperty("profile_image")
        private String profileImage;

        @JsonProperty("thumbnail_image")
        private String thumbnailImage;
    }

    @Getter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {

        @JsonProperty("name")
        private String name; // "필수 동의" 항목 (이름)

        @JsonProperty("email")
        private String email; // (null일 수 있음)

        @JsonProperty("gender")
        private String gender; // "필수 동의"

        @JsonProperty("age_range")
        private String ageRange; // "필수 동의"

        @JsonProperty("birthday")
        private String birthday; // "필수 동의" (MMDD)

        @JsonProperty("birthyear")
        private String birthyear; // "필수 동의" (YYYY)
    }

    // == 편의 메서드 == //

    // [추가] 프로필 이미지를 가져오기 위한 편의 메서드
    public String getProfileImageUrl() {
        if (this.properties != null) {
            if (this.properties.getProfileImage() != null) {
                return this.properties.getProfileImage();
            }
            // 프로필 이미지가 없으면 썸네일 이미지 사용
            if (this.properties.getThumbnailImage() != null) {
                return this.properties.getThumbnailImage();
            }
        }
        return null; // 둘 다 없으면 null 반환
    }
}