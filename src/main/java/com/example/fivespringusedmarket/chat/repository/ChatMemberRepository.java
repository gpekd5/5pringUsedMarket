package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember,Long> {
}
