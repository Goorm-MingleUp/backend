package com.mingleup.backend.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Spring Boot 기본 빈 주입

    private static final String KAKAO_SEND_ME_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    // --- 1. 파티 신청 결과 알림 ---
    public void sendApplicationResultNotification(User recipient, Party party, ApplicationStatus status) {
        String resultText = (status == ApplicationStatus.APPROVED) ? "승인되었습니다! 🎉" : "아쉽게도 거절되었습니다.";
        String message = String.format("['%s' 파티 신청 결과]\n\n결과: %s\n일시: %s",
                party.getTitle(), resultText, party.getPartyDatetime());

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/parties/" + party.getId());
    }

    // --- 2. AI 매칭 결과 알림 ---
    public void sendPartyFinalizationNotification(User recipient, Party party, AiGroup group) {
        if (group == null) return;
        String reason = (group.getMatchingReason() != null) ? group.getMatchingReason() : "공통 관심사 매칭";
        String message = String.format("['%s' 파티 확정 & 매칭]\n\n나의 조: %s\n사유: %s\n\n즐거운 시간 되세요!",
                party.getTitle(), group.getGroupName(), reason);

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/parties/" + party.getId());
    }

    // --- 3. 후기 작성 요청 알림 ---
    public void sendReviewRequestNotification(User recipient, Party party) {
        String message = String.format("['%s' 파티는 즐거우셨나요?]\n\n소중한 후기를 남겨주세요!\n호스트와 참가자들에게 큰 힘이 됩니다.",
                party.getTitle());

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/reviews/create?partyId=" + party.getId());
    }

    // --- [핵심] 카카오톡 전송 로직 ---
    private void sendKakaoMemo(User recipient, String text, String linkUrl) {
        if (recipient.getKakaoAccessToken() == null) {
            log.warn(">>> [전송 실패] 유저 '{}'의 카카오 토큰이 없습니다.", recipient.getName());
            // TODO: 여기서 Refresh Token을 이용해 Access Token을 갱신하는 로직을 호출하거나, 실패 처리해야 함
            return;
        }

        try {
            // 1. DTO 생성 (빌더 패턴 활용)
            KakaoTemplateObject templateObject = KakaoTemplateObject.builder()
                    .objectType("text")
                    .text(text)
                    .link(new KakaoLink(linkUrl, linkUrl))
                    .buttonTitle("자세히 보기")
                    .build();

            // 2. 객체를 JSON String으로 변환 (특수문자 자동 이스케이프 처리됨)
            String jsonTemplate = objectMapper.writeValueAsString(templateObject);

            // 3. 파라미터 세팅
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("template_object", jsonTemplate);

            // 4. API 호출
            webClient.post()
                    .uri(KAKAO_SEND_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + recipient.getKakaoAccessToken())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> log.info(">>> [카카오톡 전송 성공] To: {}", recipient.getName()),
                            error -> handleError(error, recipient)
                    );

        } catch (JsonProcessingException e) {
            log.error(">>> [JSON 변환 에러] 메시지 생성 중 오류 발생", e);
        }
    }

    private void handleError(Throwable error, User recipient) {
        log.error(">>> [카카오톡 전송 실패] To: {}", recipient.getName());
        if (error instanceof WebClientResponseException ex) {
            log.error("Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            // TODO: 401 Unauthorized 에러일 경우 토큰 갱신 로직 트리거 필요
        } else {
            log.error("Error: {}", error.getMessage());
        }
    }

    // --- 내부 DTO 클래스 (Inner Classes) ---
    // 카카오 메시지 규격에 맞춘 DTO입니다.

    @Getter
    @Builder
    static class KakaoTemplateObject {
        @com.fasterxml.jackson.annotation.JsonProperty("object_type")
        private String objectType;

        private String text;

        private KakaoLink link;

        @com.fasterxml.jackson.annotation.JsonProperty("button_title")
        private String buttonTitle;
    }

    @Getter
    @RequiredArgsConstructor
    static class KakaoLink {
        @com.fasterxml.jackson.annotation.JsonProperty("web_url")
        private final String webUrl;

        @com.fasterxml.jackson.annotation.JsonProperty("mobile_web_url")
        private final String mobileWebUrl;
    }
}