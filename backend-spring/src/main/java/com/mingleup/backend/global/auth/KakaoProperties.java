package com.mingleup.backend.global.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application-secret.yml의 'kakao' 프로퍼티를 바인딩하는 클래스
 */
@Component
@ConfigurationProperties(prefix = "kakao")
@Getter
@Setter
public class KakaoProperties {
    // Auth
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUri;
    private String userInfoUri;
    private String authUri;

    // [추가] Notification (알림톡용)
    private String adminKey;      // 카카오 어드민 키 (서버에서 발송 시 필요)
    private String senderNumber;  // 발신 번호 (알림톡 필수)
}