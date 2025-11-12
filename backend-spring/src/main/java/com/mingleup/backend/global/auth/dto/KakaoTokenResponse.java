package com.mingleup.backend.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 토큰 응답 DTO
 * (WebClient가 JSON을 객체로 매핑하기 위해 사용)
 */
@Getter
@NoArgsConstructor
public class KakaoTokenResponse {

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    @JsonProperty("scope")
    private String scope;
}

