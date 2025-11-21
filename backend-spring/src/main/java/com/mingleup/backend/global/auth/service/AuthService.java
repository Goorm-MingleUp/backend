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
     * 1. 인가 코드로 카카오 액세스 토큰 받기
     * 2. 액세스 토큰으로 카카오 사용자 정보 받기
     * 3. DB에서 사용자 조회 또는 신규 등록
     * 4. MingleUp JWT 발급
     * @param code 카카오 인가 코드
     * @return MingleUp JWT와 사용자 정보가 담긴 DTO
     */
    public LoginResponse processKakaoLogin(String code) {
        // 1. 인가 코드로 액세스 토큰 받기
        KakaoTokenResponse tokenResponse = getKakaoToken(code);
        log.info("카카오 액세스 토큰 수신: {}", tokenResponse.getAccessToken());

        // 2. 액세스 토큰으로 사용자 정보 받기
        KakaoUserInfoResponse userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());
        log.info("카카오 사용자 정보 수신: {}", userInfo.getKakaoId());

        // 3. 사용자 조회 또는 신규 등록 (isNewUser 값은 이제 사용 안함)
        User user = saveOrUpdateUser(userInfo);
        log.info("MingleUp 사용자 저장/조회: {}", user.getId());

        // 4. MingleUp JWT 발급
        String jwtToken = jwtTokenProvider.createToken(user.getId(), user.getRole());
        log.info("MingleUp JWT 토큰 생성 완료");

        return new LoginResponse(jwtToken, user.getId(), user.getName());
    }

    /**
     * (서버 to 서버) 카카오에 액세스 토큰 요청
     */
    private KakaoTokenResponse getKakaoToken(String code) {
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
                .block(); // 동기 방식
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
                .block(); // 동기 방식
    }

    /**
     * 카카오 ID로 사용자를 조회하고, 없으면 새로 생성하여 저장
     */
    public User saveOrUpdateUser(KakaoUserInfoResponse userInfo) {
        Optional<User> existingUser = userRepository.findByKakaoId(userInfo.getKakaoId());

        if (existingUser.isPresent()) {
            log.debug("Existing user found: {}", existingUser.get().getId());
            return existingUser.get();
        }

        log.debug("New user. Saving to database.");

        KakaoUserInfoResponse.KakaoAccount account = userInfo.getKakaoAccount();

        // 생년월일 조합
        LocalDate birthdate = null;
        if (account.getBirthyear() != null && account.getBirthday() != null) {
            try {
                // YYYY와 MMDD를 조합하여 YYYY-MM-DD 형식의 LocalDate 생성
                birthdate = LocalDate.parse(
                        account.getBirthyear() + "-" +
                                account.getBirthday().substring(0, 2) + "-" +
                                account.getBirthday().substring(2, 4)
                );
            } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                log.warn("Failed to parse birthdate (yyyy:{}, mmdd:{}): {}",
                        account.getBirthyear(), account.getBirthday(), e.getMessage());
            }
        }

        // 성별 변환
        Gender gender = null;
        if (account.getGender() != null) {
            gender = account.getGender().equalsIgnoreCase("male") ? Gender.MALE :
                    account.getGender().equalsIgnoreCase("female") ? Gender.FEMALE :
                            Gender.OTHER;
        }

        User newUser = User.builder()
                .kakaoId(userInfo.getKakaoId())
                .name(account.getName()) // '이름'
                .email(account.getEmail()) // '이메일' (null일 수 있음)
                .profileImageUrl(userInfo.getProfileImageUrl()) // '프로필 사진' (null일 수 있음)
                .gender(gender)
                .birthdate(birthdate)
                .role(Role.PARTICIPANT) // 기본 역할
                .build();

        return userRepository.save(newUser);
    }
}