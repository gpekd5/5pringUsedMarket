package com.example.fivespringusedmarket.search.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.search.metrics.SearchCacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/cache-stats")
public class SearchCacheStatsController {

    private final SearchCacheStats searchCacheStats;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> response = Map.of(
                "caffeineHit", searchCacheStats.getCaffeineHit(),
                "caffeineMiss", searchCacheStats.getCaffeineMiss(),
                "caffeineTotal", searchCacheStats.getCaffeineTotal(),
                "caffeineHitRatio", searchCacheStats.getCaffeineHitRatio(),

                "redisHit", searchCacheStats.getRedisHit(),
                "redisMiss", searchCacheStats.getRedisMiss(),
                "redisTotal", searchCacheStats.getRedisTotal(),
                "redisHitRatio", searchCacheStats.getRedisHitRatio()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> reset() {
        searchCacheStats.reset();

        return ResponseEntity.ok(ApiResponse.success("캐시 통계가 초기화되었습니다.", null));
    }
}