package com.mingleup.backend.global.auth.service;

import com.mingleup.backend.domain.user.domain.Gender;
import com.mingleup.backend.domain.user.domain.Role;
import com.mingleup.backend.domain.user.domain.User;
import com.mingleup.backend.domain.user.repository.UserRepository;
import com.mingleup.backend.global.auth.JwtTokenProvider;
import com.mingleup.backend.global.auth.KakaoProperties;
import com.mingleup.backend.global.auth.dto.KakaoTokenResponse;
import com.mingleup.backend.global.auth.dto.KakaoUserInfoResponse;
import com.mingleup.backend.global.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * 카카오 로그인/회원가입 처리를 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final WebClient webClient;
    private final KakaoProperties kakaoProperties;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 카카오 로그인/회원가입 전체 프로세스
     */
    public LoginResponse processKakaoLogin(String code) {
        // 1. 인가 코드로 액세스 토큰 받기
        KakaoTokenResponse tokenResponse = getKakaoTokenResponse(code);
        String kakaoAccessToken = tokenResponse.getAccessToken();
        log.info("카카오 액세스 토큰 수신 완료");

        // 2. 액세스 토큰으로 사용자 정보 받기
        KakaoUserInfoResponse userInfo = getKakaoUserInfo(kakaoAccessToken);
        log.info("카카오 사용자 정보 수신: {}", userInfo.getKakaoId());

        // 3. 사용자 조회 또는 신규 등록 (Kakao Token 저장)
        User user = saveOrUpdateUser(userInfo, kakaoAccessToken);
        log.info("MingleUp 사용자 저장/조회 완료: {}", user.getId());

        // 4. MingleUp JWT 발급
        String jwtToken = jwtTokenProvider.createToken(user.getId(), user.getRole());

        // [핵심 수정] 개발 편의를 위해 콘솔에 JWT 토큰 출력 (Swagger 테스트용)
        log.info("=========================================================");
        log.info(">>> 생성된 JWT 토큰 (아래 값을 복사해서 Swagger에 넣으세요):");
        log.info("Bearer {}", jwtToken);
        log.info("=========================================================");

        return new LoginResponse(jwtToken, user.getId(), user.getName());
    }

    /**
     * (서버 to 서버) 카카오에 액세스 토큰 요청
     */
    private KakaoTokenResponse getKakaoTokenResponse(String code) {
        String tokenUri = kakaoProperties.getTokenUri();
        String requestBody = "grant_type=authorization_code"
                + "&client_id=" + kakaoProperties.getClientId()
                + "&client_secret=" + kakaoProperties.getClientSecret()
                + "&redirect_uri=" + kakaoProperties.getRedirectUri()
                + "&code=" + code;

        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    /**
     * (서버 to 서버) 카카오에 사용자 정보 요청
     */
    private KakaoUserInfoResponse getKakaoUserInfo(String accessToken) {
        String userInfoUri = kakaoProperties.getUserInfoUri();

        return webClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();
    }

    /**
     * 카카오 ID로 사용자를 조회하고, 없으면 새로 생성하여 저장
     */
    public User saveOrUpdateUser(KakaoUserInfoResponse userInfo, String kakaoAccessToken) {
        Optional<User> existingUser = userRepository.findByKakaoId(userInfo.getKakaoId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateKakaoToken(kakaoAccessToken);
            return user;
        }

        log.debug("New user. Saving to database.");

        KakaoUserInfoResponse.KakaoAccount account = userInfo.getKakaoAccount();

        LocalDate birthdate = null;
        if (account.getBirthyear() != null && account.getBirthday() != null) {
            try {
                birthdate = LocalDate.parse(
                        account.getBirthyear() + "-" +
                                account.getBirthday().substring(0, 2) + "-" +
                                account.getBirthday().substring(2, 4)
                );
            } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                log.warn("Failed to parse birthdate: {}", e.getMessage());
            }
        }

        Gender gender = null;
        if (account.getGender() != null) {
            gender = account.getGender().equalsIgnoreCase("male") ? Gender.MALE :
                    account.getGender().equalsIgnoreCase("female") ? Gender.FEMALE :
                            Gender.OTHER;
        }

        User newUser = User.builder()
                .kakaoId(userInfo.getKakaoId())
                .name(account.getName())
                .email(account.getEmail())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .gender(gender)
                .birthdate(birthdate)
                .role(Role.PARTICIPANT)
                .build();

        newUser.updateKakaoToken(kakaoAccessToken);

        return userRepository.save(newUser);
    }
}