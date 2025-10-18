package com.agent_chat.agent_chat.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agent_chat.agent_chat.Entity.Message;
import com.agent_chat.agent_chat.Entity.Message.MessageRole;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatIdChatOrderByCreatedAtAsc(UUID chatId);

    List<Message> findByChatIdChatAndRoleOrderByCreatedAtAsc(UUID chatId, MessageRole role);

    long countByChatIdChat(UUID chatId);

    @Query("SELECT m FROM Message m WHERE m.chat.idChat = :chatId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessageByChatId(@Param("chatId") UUID chatId);

    void deleteByChatIdChat(UUID chatId);
}
