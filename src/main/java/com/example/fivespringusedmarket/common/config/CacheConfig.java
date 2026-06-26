package com.example.fivespringusedmarket.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        // caffein 기반 cacheManager 생성
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("productSearch");

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5)) // 캐시 저장 후 5분이 지나면 만료
                        .maximumSize(500) // 캐시는 최대 500개까지 저장
        );

        return cacheManager; // 위 설정이 적용된 cacheManager 반환
    }
}
