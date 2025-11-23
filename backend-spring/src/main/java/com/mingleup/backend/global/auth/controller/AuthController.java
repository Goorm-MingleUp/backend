package com.mingleup.backend.global.auth.controller;

import com.mingleup.backend.global.auth.KakaoProperties;
import com.mingleup.backend.global.auth.dto.LoginResponse;
import com.mingleup.backend.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
    @Operation(
            summary = "카카오 로그인 시작 (토큰 발급)",
            description = """
                    **[토큰을 받아오려면 이 API를 호출하세요]**
                    http://localhost:8080/api/v1/auth/kakao/login

                    클라이언트(브라우저)에서 이 API를 호출하면, 사용자는 카카오 로그인 페이지로 리디렉션됩니다.

                    로그인 성공 후, 사용자는 /api/v1/auth/kakao/callback으로 리디렉션되고, 서버는 최종적으로 프론트엔드 URL(http://localhost:3000/auth-redirect)로 JWT 토큰과 사용자 정보를 쿼리 파라미터에 담아 리디렉션시킵니다.
                    """,
            tags = {"Auth"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리디렉션 (Found)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
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

        // [필수 추가] 이 줄을 꼭 추가해야 동의 화면에 뜹니다!
        scopeJoiner.add("talk_message");

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
    @Operation(
            summary = "카카오 로그인 콜백 (내부 처리용)",
            description = "카카오 서버가 인가 코드를 들고 이 API를 호출합니다. (클라이언트가 직접 호출 X)\n" +
                    "성공 시 프론트엔드 URL로 JWT 토큰과 함께 리디렉션됩니다.",
            tags = {"Auth"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "프론트엔드 URL로 리디렉션 (Found)"),
            @ApiResponse(responseCode = "500", description = "카카오 로그인 처리 실패 (프론트엔드 에러 페이지로 리디렉션)")
    })
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> handleKakaoCallback(@RequestParam("code") String code) {
        log.info("카카오 로그인 요청 (인가 코드 수신): {}", code);

        // 1. 토큰 발급
        LoginResponse loginResponse = authService.processKakaoLogin(code);
        String jwtToken = loginResponse.getJwtToken();

        // 2. 쿠키 생성 (HttpOnly 설정으로 자바스크립트 접근 불가 -> 보안 강화)
        ResponseCookie cookie = ResponseCookie.from("accessToken", jwtToken)
                .httpOnly(true)          // JS에서 쿠키 접근 불가능 (XSS 방지)
                .secure(false)           // https 적용 시 true로 변경 (로컬은 false)
                .path("/")               // 모든 경로에서 쿠키 전송
                .maxAge(60 * 60)         // 1시간 (토큰 만료시간과 맞춤)
                .sameSite("Lax")         // CSRF 보호 및 리다이렉트 시 쿠키 유지
                .build();

        // 3. 프론트엔드 메인으로 리다이렉트 (헤더에 쿠키 포함)
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("http://localhost:3000/home")) // 프론트엔드 홈 URL
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}