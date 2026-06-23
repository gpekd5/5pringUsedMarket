package com.example.fivespringusedmarket.member.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 정보를 저장하는 JPA 엔티티다.
 * 이메일과 닉네임은 서비스 내에서 유일해야 한다.
 */
@Getter
@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    private Member(String email, String password, String nickname, MemberRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    public static Member create(String email, String encodedPassword, String nickname) {
        // 신규 가입 회원은 기본 권한을 MEMBER로 생성한다.
        return new Member(email, encodedPassword, nickname, MemberRole.MEMBER);
    }
}
