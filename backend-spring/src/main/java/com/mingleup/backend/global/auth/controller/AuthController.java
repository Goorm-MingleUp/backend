package com.mingleup.backend.global.auth.controller;

import com.mingleup.backend.global.auth.KakaoProperties;
import com.mingleup.backend.global.auth.dto.LoginResponse;
import com.mingleup.backend.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthService authService;
    private final String clientRedirectUrl = "http://localhost:3000"; // 프론트엔드 주소 (임시)

    /**
     * 클라이언트를 카카오 인가 코드 발급 페이지로 리디렉션
     * [수정] scope 파라미터를 카카오 콘솔의 "필수 동의" 항목으로만 구성
     */
    @GetMapping("/kakao/login")
    public ResponseEntity<Void> redirectToKakaoLogin() {
        // [수정] "사용 안 함" 항목(profile_nickname, profile_image, account_email) 제거
        // [추가] "필수 동의" 항목(name) 추가
        List<String> scopes = List.of("name", "gender", "age_range", "birthday", "birthyear");
        String scopeParam = scopes.stream().collect(Collectors.joining(","));

        String authUrl = kakaoProperties.getAuthUri()
                + "?client_id=" + kakaoProperties.getClientId()
                + "&redirect_uri=" + kakaoProperties.getRedirectUri()
                + "&response_type=code"
                + "&scope=" + scopeParam;

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302
    }

    /**
     * 카카오 인가 코드를 받아 로그인 처리
     * @param code 카카오가 발급한 인가 코드
     * @return 성공 시 프론트 주소로 JWT 토큰과 함께 리디렉션, 실패 시 에러 페이지로 리디렉션
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> handleKakaoCallback(@RequestParam("code") String code) {
        HttpHeaders headers = new HttpHeaders();
        try {
            log.info("카카오 인가 코드 수신: {}", code);
            LoginResponse loginResponse = authService.processKakaoLogin(code);

            // 성공: JWT 토큰을 쿼리 파라미터로 프론트 리디렉션
            String redirectUrl = clientRedirectUrl + "/auth-redirect?token=" + loginResponse.getJwtToken();
            headers.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 에러 발생", e);
            // 실패: 프론트 에러 페이지로 리디렉션
            String errorUrl = clientRedirectUrl + "/login-error"; // 프론트의 에러 페이지 (임시)
            headers.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}