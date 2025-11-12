package com.mingleup.backend.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 성공 시 클라이언트에 반환하는 DTO
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String jwtToken;
    private Long userId;
    private String name;
}