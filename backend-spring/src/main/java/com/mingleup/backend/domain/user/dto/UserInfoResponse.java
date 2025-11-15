package com.mingleup.backend.domain.user.dto;

import com.mingleup.backend.domain.user.domain.Gender;
import com.mingleup.backend.domain.user.domain.Role;
import com.mingleup.backend.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * '내 정보 조회' (GET /api/v1/users/me) 응답을 위한 DTO
 */
@Getter
public class UserInfoResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String profileImageUrl;
    private final Gender gender;
    private final LocalDate birthdate;
    private final Role role;
    private final String region;
    private final String mbti;
    private final List<String> hobbies;
    private final List<String> idealTypeHobbies;
    private final String hostNickname;

    @Builder
    public UserInfoResponse(Long id, String name, String email, String profileImageUrl, Gender gender, LocalDate birthdate, Role role, String region, String mbti, List<String> hobbies, List<String> idealTypeHobbies, String hostNickname ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.birthdate = birthdate;
        this.role = role;
        this.region = region;
        this.mbti = mbti;
        this.hobbies = hobbies;
        this.idealTypeHobbies = idealTypeHobbies;
        this.hostNickname = hostNickname;
    }

    /**
     * User 엔티티를 MyInfoResponse DTO로 변환하는 정적 팩토리 메서드
     */
    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .role(user.getRole())
                .region(user.getRegion())
                .mbti(user.getMbti())
                .hobbies(user.getHobbies())
                .idealTypeHobbies(user.getIdealTypeHobbies())
                .hostNickname(user.getHostNickname())
                .build();
    }
}