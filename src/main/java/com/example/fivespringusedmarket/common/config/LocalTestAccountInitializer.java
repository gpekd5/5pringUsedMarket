package com.example.fivespringusedmarket.common.config;

import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경에서 테스트용 회원 계정을 자동 생성한다.
 * 도메인 엔티티에 테스트 전용 생성 메서드를 추가하지 않기 위해 JDBC로만 처리한다.
 */
@Profile("local")
@Component
public class LocalTestAccountInitializer implements ApplicationRunner {

    private static final String TEST_PASSWORD = "Password123!";
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_NICKNAME = "관리자";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MEMBER_EMAIL = "member@test.com";
    private static final String MEMBER_NICKNAME = "일반회원";
    private static final String MEMBER_ROLE = "MEMBER";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public LocalTestAccountInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTestAccountIfNotExists(ADMIN_EMAIL, ADMIN_NICKNAME, ADMIN_ROLE);
        createTestAccountIfNotExists(MEMBER_EMAIL, MEMBER_NICKNAME, MEMBER_ROLE);
    }

    private void createTestAccountIfNotExists(String email, String nickname, String role) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from members where email = ?",
                Integer.class,
                email
        );

        if (count != null && count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // local 프로필에서만 테스트용 계정을 생성한다.
        jdbcTemplate.update(
                """
                        insert into members
                            (email, password, nickname, role, created_at, updated_at)
                        values
                            (?, ?, ?, ?, ?, ?)
                        """,
                email,
                passwordEncoder.encode(TEST_PASSWORD),
                nickname,
                role,
                now,
                now
        );
    }
}
