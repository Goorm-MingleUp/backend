package com.mingleup.backend.global.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application-secret.yml의 'jwt' 프로퍼티를 바인딩하는 클래스
 */
@Component
@ConfigurationProperties(prefix = "jwt") // "jwt.secret", "jwt.expirationInMs"와 매핑
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expirationInMs;
}