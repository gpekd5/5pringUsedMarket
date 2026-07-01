package com.example.fivespringusedmarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션 컨텍스트 로딩을 검증하는 기본 테스트다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=1209600000"
})
class FivespringUsedMarketApplicationTests {

    @Test
    void contextLoads() {
        // Spring ApplicationContext가 정상적으로 로딩되는지 확인한다.
    }
}
