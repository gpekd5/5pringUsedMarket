package com.example.fivespringusedmarket.search.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class SearchCacheStats {

    private final AtomicLong caffeineHit = new AtomicLong();
    private final AtomicLong caffeineMiss = new AtomicLong();

    private final AtomicLong redisHit = new AtomicLong();
    private final AtomicLong redisMiss = new AtomicLong();

    public void increaseCaffeineHit() {
        caffeineHit.incrementAndGet();
    }

    public void increaseCaffeineMiss() {
        caffeineMiss.incrementAndGet();
    }

    public void increaseRedisHit() {
        redisHit.incrementAndGet();
    }

    public void increaseRedisMiss() {
        redisMiss.incrementAndGet();
    }

    public long getCaffeineHit() {
        return caffeineHit.get();
    }

    public long getCaffeineMiss() {
        return caffeineMiss.get();
    }

    public long getCaffeineTotal() {
        return getCaffeineHit() + getCaffeineMiss();
    }

    public double getCaffeineHitRatio() {
        long total = getCaffeineTotal();

        if (total == 0) {
            return 0.0;
        }

        return (double) getCaffeineHit() / total * 100;
    }

    public long getRedisHit() {
        return redisHit.get();
    }

    public long getRedisMiss() {
        return redisMiss.get();
    }

    public long getRedisTotal() {
        return getRedisHit() + getRedisMiss();
    }

    public double getRedisHitRatio() {
        long total = getRedisTotal();

        if (total == 0) {
            return 0.0;
        }

        return (double) getRedisHit() / total * 100;
    }

    public void reset() {
        caffeineHit.set(0);
        caffeineMiss.set(0);
        redisHit.set(0);
        redisMiss.set(0);
    }
}