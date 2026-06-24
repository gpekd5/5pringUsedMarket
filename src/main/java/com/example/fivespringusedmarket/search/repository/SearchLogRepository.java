package com.example.fivespringusedmarket.search.repository;

import com.example.fivespringusedmarket.search.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
