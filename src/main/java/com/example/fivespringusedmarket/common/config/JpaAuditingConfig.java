package com.example.fivespringusedmarket.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing을 활성화해 BaseEntity의 생성/수정 시간을 자동으로 기록한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
