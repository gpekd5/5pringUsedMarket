package com.example.fivespringusedmarket.auth.repository;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 로그아웃된 Access Token을 Redis Blacklist로 관리한다.
 */
@Repository
public class AccessTokenBlacklistRepository {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:access-token:";
    private static final String BLACKLIST_VALUE = "logout";

    private final StringRedisTemplate stringRedisTemplate;

    public AccessTokenBlacklistRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(String accessToken, long ttlMillis) {
        stringRedisTemplate.opsForValue()
                .set(getKey(accessToken), BLACKLIST_VALUE, Duration.ofMillis(ttlMillis));
    }

    public boolean exists(String accessToken) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getKey(accessToken)));
    }

    private String getKey(String accessToken) {
        return BLACKLIST_KEY_PREFIX + accessToken;
    }
}
