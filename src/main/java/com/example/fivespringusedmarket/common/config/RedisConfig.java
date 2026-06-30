package com.example.fivespringusedmarket.common.config;

import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * Redis Cache 설정 클래스입니다.
 *
 * <p>검색 V3 API에서 사용하는 Redis 기반 Remote Cache를 설정합니다.
 *
 * <p>Redis key는 검색 조건을 조합한 문자열이므로
 * 사람이 읽기 쉬운 형태로 저장하기 위해 {@link StringRedisSerializer}를 사용합니다.
 *
 * <p>Redis value는 검색 결과 응답 DTO를 저장해야 하므로
 * JSON 직렬화를 위해 {@link GenericJacksonJsonRedisSerializer}를 사용합니다.
 *
 * <p>검색 결과 응답에는 LocalDateTime 필드가 포함될 수 있으므로,
 * JsonMapper를 통해 날짜/시간 타입이 문자열 형태로 직렬화되도록 설정합니다.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean(name = "redisCacheManager") // v3 검색 캐시에서 명시적으로 사용할 이름 설정
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        JsonMapper jsonMapper = JsonMapper.builder()
                .build(); //  Jackson 3용 JsonMapper 생성

        // 검색 결과 DTO를 JSON으로 저장하기 위한 Serializer
        JacksonJsonRedisSerializer<ProductPageResponse> valueSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, ProductPageResponse.class);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 검색 결과 캐시 TTL 5분 설정
                .disableCachingNullValues() // null 결과 캐싱 안함
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()) // Redis key는 문자열로 저장
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer) // Redis value는 JSON으로 저장
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
