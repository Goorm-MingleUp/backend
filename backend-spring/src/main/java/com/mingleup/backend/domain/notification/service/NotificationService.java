package com.mingleup.backend.domain.notification.service;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // [추가]
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

    // 카카오 나에게 보내기 API URL
    private static final String KAKAO_SEND_ME_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * 1. 파티 신청 결과 알림 (승인/거절)
     */
    public void sendApplicationResultNotification(User recipient, Party party, ApplicationStatus status) {
        String resultText = (status == ApplicationStatus.APPROVED) ? "승인되었습니다! 🎉" : "아쉽게도 거절되었습니다.";
        String message = String.format(
                "['%s' 파티 신청 결과]\\n\\n결과: %s\\n일시: %s",
                party.getTitle(), resultText, party.getPartyDatetime()
        );

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/parties/" + party.getId());
    }

    /**
     * 2. AI 매칭 결과 알림 (조 편성)
     */
    public void sendPartyFinalizationNotification(User recipient, Party party, AiGroup group) {
        if (group == null) return;

        String reason = (group.getMatchingReason() != null) ? group.getMatchingReason() : "공통 관심사 매칭";
        String message = String.format(
                "['%s' 파티 확정 & 매칭]\\n\\n나의 조: %s\\n사유: %s\\n\\n즐거운 시간 되세요!",
                party.getTitle(), group.getGroupName(), reason
        );

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/parties/" + party.getId());
    }

    /**
     * 3. 후기 작성 요청 알림
     */
    public void sendReviewRequestNotification(User recipient, Party party) {
        String message = String.format(
                "['%s' 파티는 즐거우셨나요?]\\n\\n소중한 후기를 남겨주세요!\\n호스트와 참가자들에게 큰 힘이 됩니다.\\n\\n(아래 버튼을 눌러 작성 페이지로 이동하세요)",
                party.getTitle()
        );

        sendKakaoMemo(recipient, message, frontendBaseUrl + "/reviews/create?partyId=" + party.getId());
    }

    /**
     * [핵심] 카카오 '나에게 보내기' API 호출
     */
    private void sendKakaoMemo(User recipient, String text, String linkUrl) {
        if (recipient.getKakaoAccessToken() == null) {
            log.warn(">>> [전송 건너뜀] 유저 '{}'의 카카오 토큰이 없습니다.", recipient.getName());
            return;
        }

        String templateObject = String.format(
                "{" +
                        "\"object_type\": \"text\"," +
                        "\"text\": \"%s\"," +
                        "\"link\": {" +
                        "\"web_url\": \"%s\"," +
                        "\"mobile_web_url\": \"%s\"" +
                        "}," +
                        "\"button_title\": \"자세히 보기\"" +
                        "}",
                text.replace("\n", "\\n"), linkUrl, linkUrl
        );

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateObject);

        webClient.post()
                .uri(KAKAO_SEND_ME_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + recipient.getKakaoAccessToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info(">>> [카카오톡 전송 성공] To: {}", recipient.getName()),
                        error -> {
                            log.error(">>> [카카오톡 전송 실패] To: {}", recipient.getName());
                            if (error instanceof WebClientResponseException ex) {
                                log.error("Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                            } else {
                                log.error("Error: {}", error.getMessage());
                            }
                        }
                );
    }
}