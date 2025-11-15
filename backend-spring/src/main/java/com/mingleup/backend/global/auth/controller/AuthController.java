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
import java.net.URLEncoder; // [추가]
import java.nio.charset.StandardCharsets; // [추가]
import java.util.StringJoiner;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthService authService;

    /**
     * 카카오 로그인 페이지로 리디렉션
     * @return
     */
    @GetMapping("/kakao/login")
    public ResponseEntity<Void> redirectToKakaoLogin() {
        // 동의 항목 범위(scope) 설정
        StringJoiner scopeJoiner = new StringJoiner(",");
        scopeJoiner.add("profile_image"); // 프로필 사진 (선택 동의)
        scopeJoiner.add("name"); // 이름 (필수 동의)
        scopeJoiner.add("gender"); // 성별 (필수 동의)
        scopeJoiner.add("birthday"); // 생일 (필수 동의)
        scopeJoiner.add("birthyear"); // 출생 연도 (필수 동의)
        // 참고: age_range는 birthday, birthyear가 있으면 자동 추론 가능하여 별도 요청 안함

        String authUrl = kakaoProperties.getAuthUri()
                + "?client_id=" + kakaoProperties.getClientId()
                + "&redirect_uri=" + kakaoProperties.getRedirectUri()
                + "&response_type=code"
                + "&scope=" + scopeJoiner.toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authUrl));

        // 302 Found 응답을 보내 브라우저를 리디렉션
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 카카오 로그인 콜백 처리
     * @param code 카카오가 발급한 인가 코드
     * @return
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> handleKakaoCallback(@RequestParam("code") String code) {
        log.info("카카오 인가 코드 수신: {}", code);
        String frontendRedirectUrl = "http://localhost:3000/auth-redirect";
        String errorUrl = "http://localhost:3000/login-error";

        try {
            // AuthService를 통해 카카오 로그인 처리 및 MingleUp JWT 발급
            LoginResponse loginResponse = authService.processKakaoLogin(code);

            // [수정] 성공 시 token, userId, name을 URL에 추가
            String successUrl = frontendRedirectUrl
                    + "?token=" + loginResponse.getJwtToken()
                    + "&userId=" + loginResponse.getUserId()
                    + "&name=" + URLEncoder.encode(loginResponse.getName(), StandardCharsets.UTF_8); // 이름 URL 인코딩

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(successUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302

        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 에러 발생", e);
            // 실패 시 에러 페이지로 리디렉션
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}