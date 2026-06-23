package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
