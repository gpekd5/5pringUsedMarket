package com.example.fivespringusedmarket.search.cache;

import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 *  검색 조건과 페이징 정보를 기반으로 캐시 key 생성하는 클래스
 */
@Component
public class SearchCacheKeyGenerator {

    public String generate(ProductSearchCondition condition, Pageable pageable) {

        // 같은 검색 조건이면 같은 key가 생성되어야 캐시가 동작
        // 조건이 많기 때문에 클래스를 따로 가지고감
        return String.join(":",
                "keyword=" + nullToAll(condition.keyword()),
                "category=" + nullToAll(condition.category()),
                "status=" + nullToAll(condition.status()),
                "sortType=" + nullToAll(condition.sort()),
                "page=" + nullToAll(pageable.getPageNumber()),
                "size=" + nullToAll(pageable.getPageSize()),
                "sort=" + nullToAll(pageable.getSort())
        );

    }

    private String nullToAll(Object value) {
        // null 값을 그대로 사용하면 Key가 이상해져 all로 치환
        return value == null ? "ALL" : value.toString();
    }
}
