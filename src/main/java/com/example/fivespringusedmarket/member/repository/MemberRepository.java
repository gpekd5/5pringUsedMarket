package com.example.fivespringusedmarket.member.repository;

import com.example.fivespringusedmarket.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 엔티티의 DB 접근을 담당한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
