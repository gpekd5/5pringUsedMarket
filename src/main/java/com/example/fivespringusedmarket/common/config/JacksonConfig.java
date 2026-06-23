package com.example.fivespringusedmarket.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 직렬화에 사용하는 Jackson ObjectMapper를 공통 Bean으로 등록한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Security Filter 단계에서도 공통 실패 응답을 JSON으로 작성하기 위해 명시적으로 등록한다.
        return new ObjectMapper();
    }
}
