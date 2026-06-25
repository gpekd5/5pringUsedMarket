package com.example.fivespringusedmarket.coupon.repository;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * 쿠폰 발급용 Redis Lock 저장소.
 * SETNX + TTL 로 Lock 을 획득하고, Lua Script 로 본인이 획득한 Lock 만 해제한다.
 */
@Repository
@RequiredArgsConstructor
public class CouponRedisLockRepository {

    // 본인이 설정한 값과 일치할 때만 삭제하는 Lua Script (원자적 비교-삭제)
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lock 획득 시도. 키가 없을 때만 value 를 세팅한다 (SETNX).
     *
     * @param key   Lock 키 (lock:coupon:{couponId})
     * @param value Lock 식별값 (UUID)
     * @return 획득 성공 시 true, 이미 Lock 이 존재하면 false
     */
    public Boolean lock(String key, String value) {
        // SET key value NX PX 3000 — 원자적으로 세팅, TTL 3초
        return stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, value, Duration.ofMillis(3_000));
    }

    /**
     * Lock 해제. Lua Script 로 value 가 일치하는 경우에만 삭제한다.
     *
     * @param key   Lock 키
     * @param value Lock 획득 시 사용한 UUID
     */
    public void unlock(String key, String value) {
        // 다른 스레드가 획득한 Lock 을 실수로 해제하지 않도록 value 검증 후 삭제
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, List.of(key), value);
    }
}
