package com.mingleup.backend.domian.user.controller;

import com.mingleup.backend.domain.user.service.UserService;
import com.mingleup.backend.domian.user.dto.MyInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 API
     * [GET] /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(Authentication authentication) {
        // JwtAuthenticationFilter에서 SecurityContext에 저장한 userId를 가져옵니다.
        // authentication.getName()에 userId(String)가 저장되어 있습니다.
        Long userId = Long.parseLong(authentication.getName());

        MyInfoResponse myInfo = userService.getMyInfo(userId);
        return ResponseEntity.ok(myInfo);
    }
}