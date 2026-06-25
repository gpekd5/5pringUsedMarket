package com.example.fivespringusedmarket.coupon.repository;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRedisLockRepository {

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    private final StringRedisTemplate stringRedisTemplate;

    public Boolean lock(String key, String value) {
        return stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, value, Duration.ofMillis(3_000));
    }

    public void unlock(String key, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), value);
    }
}
