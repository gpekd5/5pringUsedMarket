package com.example.fivespringusedmarket.chat.repository;

import com.example.fivespringusedmarket.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
