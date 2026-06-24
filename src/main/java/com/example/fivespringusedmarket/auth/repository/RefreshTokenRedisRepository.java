package com.example.fivespringusedmarket.auth.repository;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis Whitelist 방식으로 Refresh Token을 저장하고 조회한다.
 */
@Repository
public class RefreshTokenRedisRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenRedisRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(Long memberId, String refreshToken, long ttlMillis) {
        stringRedisTemplate.opsForValue()
                .set(getKey(memberId), refreshToken, Duration.ofMillis(ttlMillis));
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(getKey(memberId)));
    }

    public void deleteByMemberId(Long memberId) {
        stringRedisTemplate.delete(getKey(memberId));
    }

    private String getKey(Long memberId) {
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
