package com.mingleup.backend.domain.notification.service;

import com.mingleup.backend.domain.ai.domain.AiGroup;
import com.mingleup.backend.domain.application.domain.ApplicationStatus;
import com.mingleup.backend.domain.party.domain.Party;
import com.mingleup.backend.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WebClient webClient;

    // 카카오 나에게 보내기 API URL
    private static final String KAKAO_SEND_ME_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    @Async
    public void sendApplicationResultNotification(User recipient, Party party, ApplicationStatus status) {
        String resultText = (status == ApplicationStatus.APPROVED) ? "승인되었습니다! 🎉" : "아쉽게도 거절되었습니다.";
        String message = String.format(
                "['%s' 파티 신청 결과]\n\n결과: %s\n일시: %s",
                party.getTitle(), resultText, party.getPartyDatetime()
        );

        sendKakaoMemo(recipient, message, "https://mingleup.com"); // 링크는 임시
    }

    @Async
    public void sendPartyFinalizationNotification(User recipient, Party party, ApplicationStatus status, AiGroup group) {
        String message;
        if (status == ApplicationStatus.APPROVED && group != null) {
            message = String.format(
                    "['%s' 파티 확정 & AI 매칭]\n\n나의 조: %s\n사유: %s\n\n즐거운 시간 되세요!",
                    party.getTitle(), group.getGroupName(), group.getMatchingReason()
            );
        } else {
            message = String.format("['%s' 파티 알림]\n\n아쉽게도 최종 명단에 포함되지 못했습니다.", party.getTitle());
        }
        sendKakaoMemo(recipient, message, "https://mingleup.com");
    }

    /**
     * [핵심] 카카오 '나에게 보내기' API 호출
     */
    private void sendKakaoMemo(User recipient, String text, String webUrl) {
        if (recipient.getKakaoAccessToken() == null) {
            log.warn("유저 {}의 카카오 토큰이 없어 알림을 보낼 수 없습니다.", recipient.getName());
            return;
        }

        // 1. 메시지 템플릿 구성 (JSON 문자열)
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
                // JSON 내 줄바꿈 등 특수문자 처리가 필요할 수 있음 (간단히 구현)
                text.replace("\n", "\\n"), webUrl, webUrl
        );

        // 2. 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateObject);

        // 3. API 전송
        webClient.post()
                .uri(KAKAO_SEND_ME_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + recipient.getKakaoAccessToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> log.info("카카오 알림 전송 성공: To {}", recipient.getName()),
                        error -> log.error("카카오 알림 전송 실패: To {} (토큰 만료 가능성)", recipient.getName(), error)
                );
    }
}