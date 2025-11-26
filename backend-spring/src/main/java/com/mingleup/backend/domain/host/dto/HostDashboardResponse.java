package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class HostDashboardResponse {

    // --- 호스트 프로필 영역 ---
    private String hostName;           // 호스트 이름 (또는 닉네임)
    private String hostProfileImageUrl; // 프로필 이미지
    private String hostIntro;          // 한 줄 소개
    private BigDecimal avgRating;      // 평균 평점
    private String hostNickname;       // 호스트 닉네임

    // --- 파티 현황 카운트 영역 ---
    private Long hostedCount;          // 내가 만든 총 파티 수 (hosted)
    private Long pendingApprovalCount; // 내 파티에 대기 중인 신청자 수 (pending_approval)
    private Long completedCount;       // 완료된 파티 수 (completed)

    public static HostDashboardResponse of(User user, Long hostedCount, Long pendingApprovalCount, Long completedCount) {
        return HostDashboardResponse.builder()
                .hostName(user.getName())
                .hostProfileImageUrl(user.getProfileImageUrl())
                .hostIntro(user.getHostIntro())
                .avgRating(user.getAvgRating())
                .hostNickname(user.getHostNickname())
                .hostedCount(hostedCount)
                .pendingApprovalCount(pendingApprovalCount)
                .completedCount(completedCount)
                .build();
    }
}