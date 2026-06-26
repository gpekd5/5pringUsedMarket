package com.example.fivespringusedmarket.search.dto;

import com.example.fivespringusedmarket.search.entity.SearchLog;
import java.time.LocalDateTime;

public record RecentSearchResponse(
        Long searchLogId,
        String keyword,
        LocalDateTime createdAt
) {

    public static RecentSearchResponse from(SearchLog searchLog) {
        return new RecentSearchResponse(
                searchLog.getId(),
                searchLog.getKeyword(),
                searchLog.getCreatedAt()
        );
    }
}