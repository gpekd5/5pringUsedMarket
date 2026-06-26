package com.example.fivespringusedmarket.search.repository;

import com.example.fivespringusedmarket.search.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(Long memberId, LocalDateTime from);

    @Modifying
    @Query("""
        delete from SearchLog sl
        where sl.member.id = :memberId
        and sl.keyword = :keyword
        """)
    void deleteByMemberIdAndKeyword(@Param("memberId") Long memberId, @Param("keyword") String keyword);

}
