package com.mingleup.backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * '내 정보 수정' (PUT /api/v1/users/me) 요청을 위한 DTO
 */
@Getter
@NoArgsConstructor
public class UpdateUserInfoRequest {

    // '이름', '생년월일', '성별'은 카카오에서 받아오므로 수정 대상에서 제외

    private String region;
    private String mbti;
    private List<String> hobbies; // '나의 성향'
    private List<String> idealTypeHobbies; // '이상형의 성향'

    // Lombok이 기본 생성자를 만들지만, 테스트 등을 위한 빌더나 생성자가 필요하면 추가 가능
}