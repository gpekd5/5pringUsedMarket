package com.example.fivespringusedmarket.search.dto;

public record PopularSearchResponse(
        String keyword,
        Long searchCount
) {
}
