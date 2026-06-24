package com.example.fivespringusedmarket.search.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import com.example.fivespringusedmarket.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 상품 검색어 기록을 저장하는 엔티티입니다.
 *
 * <p>상품 검색 시 입력된 키워드를 기록하며,
 * 최근 검색어 조회와 인기 검색어 집계의 기준 데이터로 사용됩니다.</p>
 */
@Getter
@Entity
@Table(name = "search_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 100)
    private String keyword;

    public SearchLog(Member member, String keyword) {
        this.member = member;
        this.keyword = keyword;
    }
}
