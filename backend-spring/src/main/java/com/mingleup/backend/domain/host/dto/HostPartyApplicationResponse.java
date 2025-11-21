package com.mingleup.backend.domain.host.dto;

import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.application.domain.PartyApplication;
import com.mingleup.backend.domain.user.domain.Gender;
import com.mingleup.backend.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class HostPartyApplicationResponse {

    // --- 신청 정보 ---
    private Long applicationId;
    private ApplicationStatus status;
    private String answerText;       // 호스트 질문에 대한 답변
    private LocalDateTime appliedAt; // 신청 일시

    // --- 신청자(User) 프로필 정보 ---
    private Long userId;
    private String name;
    private String profileImageUrl;
    private Gender gender;
    private LocalDate birthdate;     // 혹은 나이로 변환해서 줄 수도 있음
    private String mbti;
    private String region;
    private BigDecimal avgRating;

    public static HostPartyApplicationResponse from(PartyApplication application) {
        User user = application.getUser();
        return HostPartyApplicationResponse.builder()
                .applicationId(application.getId())
                .status(application.getStatus())
                .answerText(application.getAnswerText())
                .appliedAt(application.getAppliedAt())
                .userId(user.getId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .mbti(user.getMbti())
                .region(user.getRegion())
                .avgRating(user.getAvgRating())
                .build();
    }
}