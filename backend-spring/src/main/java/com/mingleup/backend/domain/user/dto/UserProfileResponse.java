package com.mingleup.backend.domain.user.dto;

import com.mingleup.backend.domain.user.domain.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * API 4: GET /api/v1/users/{userId} (유저 프로필 조회) 응답 DTO
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답 JSON에서 제외
public class UserProfileResponse {

    private Long userId;
    private String name;
    private String profileImageUrl;
    private String hostIntro;
    private BigDecimal avgRating;
    private String hostNickname;

    /**
     * User 엔티티를 UserProfileResponse DTO로 변환합니다.
     * @param user 조회 대상 User
     * @param includeRating 평점 포함 여부 (참가자가 본인 조회 시 false)
     * @return
     */
    public static UserProfileResponse from(User user, boolean includeRating) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .hostIntro(user.getHostIntro())
                .hostNickname(user.getHostNickname())
                // [핵심 로직] includeRating이 true일 때만 평점을 설정
                .avgRating(includeRating ? user.getAvgRating() : null)
                .build();
    }
}