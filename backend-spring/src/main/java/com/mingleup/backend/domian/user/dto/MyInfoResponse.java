package com.mingleup.backend.domian.user.dto;

import com.mingleup.backend.domian.user.domain.Gender;
import com.mingleup.backend.domian.user.domain.Role;
import com.mingleup.backend.domian.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 내 정보 조회 응답 DTO
 */
@Getter
@Builder
public class MyInfoResponse {
    private Long userId;
    private String email;
    private String name;
    private Gender gender;
    private LocalDate birthdate;
    private String region;
    private String mbti;
    private List<String> hobbies;
    private String profileImageUrl;
    private Role role;
    private String hostIntro;

    // User 엔티티를 DTO로 변환
    public static MyInfoResponse from(User user) {
        return MyInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .region(user.getRegion())
                .mbti(user.getMbti())
                .hobbies(user.getHobbies())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .hostIntro(user.getHostIntro())
                .build();
    }
}