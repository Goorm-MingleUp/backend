package com.mingleup.backend.global.auth.service; // [수정] 올바른 패키지 경로

// [수정] 모든 import 경로를 'backend' 기준으로 수정
import com.mingleup.backend.domian.user.domain.Gender;
import com.mingleup.backend.domian.user.domain.Role;
import com.mingleup.backend.domian.user.domain.User;
import com.mingleup.backend.domian.user.repository.UserRepository;
import com.mingleup.backend.global.auth.JwtTokenProvider;
import com.mingleup.backend.global.auth.KakaoProperties;
import com.mingleup.backend.global.auth.dto.KakaoTokenResponse;
import com.mingleup.backend.global.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.mingleup.backend.global.auth.dto.KakaoUserInfoResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;
    private final KakaoProperties kakaoProperties;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 1. 인가 코드로 카카오 액세스 토큰 요청
     */
    private KakaoTokenResponse getKakaoToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoProperties.getClientId());
        formData.add("redirect_uri", kakaoProperties.getRedirectUri());
        formData.add("code", code);
        formData.add("client_secret", kakaoProperties.getClientSecret());

        return webClient.post()
                .uri(kakaoProperties.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(formData))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    /**
     * 2. 액세스 토큰으로 카카오 사용자 정보 요청
     */
    private KakaoUserInfoResponse getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri(kakaoProperties.getUserInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();
    }

    /**
     * 3. (MingleUp) 카카오 로그인/회원가입 처리 및 JWT 발급
     */
    @Transactional
    public LoginResponse processKakaoLogin(String code) {
        // 1. 인가 코드로 액세스 토큰 받기
        KakaoTokenResponse tokenResponse = getKakaoToken(code);
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Failed to get Kakao access token.");
        }
        log.info("카카오 액세스 토큰 수신: {}", tokenResponse.getAccessToken());

        // 2. 액세스 토큰으로 사용자 정보 받기
        KakaoUserInfoResponse userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null || userInfo.getKakaoId() == null) {
            throw new RuntimeException("Failed to get Kakao user info.");
        }
        log.info("카카오 사용자 정보 수신: {}", userInfo.getKakaoId());

        // 3. 사용자 정보로 DB 조회 또는 신규 저장
        User user = saveOrUpdateUser(userInfo);
        log.info("MingleUp 사용자 저장/조회: {}", user.getId());

        // 4. MingleUp 자체 JWT 발급
        String jwtToken = jwtTokenProvider.createToken(user.getId(), user.getRole());
        log.info("MingleUp JWT 토큰 생성 완료");

        // [수정] LoginResponse DTO에 맞게 user.getName()도 전달
        return new LoginResponse(jwtToken, user.getId(), user.getName());
    }

    /**
     * 4. (Helper) DB에서 사용자 조회 또는 신규 생성
     * (카카오 콘솔 '필수 동의' 항목 기준)
     */
    @Transactional
    public User saveOrUpdateUser(KakaoUserInfoResponse userInfo) {
        String kakaoId = userInfo.getKakaoId();

        Optional<User> existingUser = userRepository.findByKakaoId(kakaoId);
        if (existingUser.isPresent()) {
            log.debug("Existing user found: {}", existingUser.get().getId());
            return existingUser.get();
        }

        // --- 신규 사용자 등록 ---
        log.debug("New user. Saving to database.");
        KakaoUserInfoResponse.KakaoAccount account = userInfo.getKakaoAccount();
        if (account == null) {
            throw new IllegalStateException("Kakao account information is missing.");
        }

        // '이름' (필수 동의)
        String name = account.getName();

        // '프로필 이미지' (사용 안 함 -> null)
        String profileImageUrl = null;

        // '이메일' (사용 안 함 -> null일 수 있음)
        String email = account.getEmail();

        // '성별' (필수 동의)
        Gender gender = Optional.ofNullable(account.getGender())
                .map(g -> {
                    try {
                        return Gender.valueOf(g.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return Gender.OTHER;
                    }
                })
                .orElse(Gender.OTHER);

        // '생년월일' (필수 동의)
        LocalDate birthdate = null;
        if (account.getBirthyear() != null && account.getBirthday() != null && account.getBirthday().length() == 4) {
            try {
                String yyyymmdd = account.getBirthyear() + account.getBirthday(); // "YYYY" + "MMDD"
                birthdate = LocalDate.parse(yyyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                log.warn("Failed to parse birthdate (yyyy:{}, mmdd:{}): {}",
                        account.getBirthyear(), account.getBirthday(), e.getMessage());
            }
        }

        User newUser = User.builder()
                .kakaoId(kakaoId)
                .email(email)
                .name(name) // '이름' 저장
                .gender(gender)
                .birthdate(birthdate)
                .profileImageUrl(profileImageUrl) // null 저장
                .role(Role.PARTICIPANT) // 기본 역할
                .build();

        return userRepository.save(newUser);
    }
}