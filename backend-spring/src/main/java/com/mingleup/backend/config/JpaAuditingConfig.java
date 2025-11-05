package com.mingleup.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 엔티티의 createdAt, updatedAt 같은 시간 필드를 자동으로 관리해주는 역할
 * BaseTimeEntity의 Auditing 기능을 활성화합니다.
 * Auditing을 활성화하면, @CreatedDate, @LastModifiedDate 같은 어노테이션이 작동해서
 * 엔티티의 생성/수정 시간을 자동으로 기록할 수 있습니다.
 * MingleUpApplication에 @EnableJpaAuditing를 붙여도 됩니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}