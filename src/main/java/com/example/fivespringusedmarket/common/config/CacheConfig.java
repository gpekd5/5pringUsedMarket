package com.example.fivespringusedmarket.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Caffeine Cache 설정 클래스입니다.
 *
 * <p>검색 V2 API에서 사용하는 Caffeine 기반 Local Memory Cache를 설정합니다.
 *
 * <p>Caffeine Cache는 애플리케이션 서버 내부 메모리에 저장되므로
 * 단일 서버 환경에서 빠르게 동작합니다.
 *
 * <p>검색 조건 조합이 많아질 경우 메모리 사용량이 증가할 수 있으므로
 * maximumSize를 설정하여 캐시 개수를 제한합니다.
 *
 * <p>상품 정보 변경 가능성을 고려하여 TTL을 설정하고,
 * 오래된 검색 결과가 장시간 노출되지 않도록 합니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean(name = "caffeineCacheManager")
    @Primary
    public CacheManager cacheManager() {

        // caffein 기반 cacheManager 생성
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("productSearchV2");

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5)) // 캐시 저장 후 5분이 지나면 만료
                        .maximumSize(500) // 캐시는 최대 500개까지 저장
        );

        return cacheManager; // 위 설정이 적용된 cacheManager 반환
    }
}
