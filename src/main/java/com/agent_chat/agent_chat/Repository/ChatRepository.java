package com.agent_chat.agent_chat.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agent_chat.agent_chat.Entity.Chat;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

  // Tìm tất cả chats của một user
  List<Chat> findByUserIdUserOrderByUpdatedAtDesc(UUID userId);

  // Tìm chat theo ID và user ID (để đảm bảo user chỉ truy cập chat của mình)
  Optional<Chat> findByIdChatAndUserIdUser(UUID chatId, UUID userId);

  // Tìm chat với messages (eager loading)
  @Query("SELECT c FROM Chat c LEFT JOIN FETCH c.messages WHERE c.idChat = :chatId AND c.user.idUser = :userId")
  Optional<Chat> findByIdChatAndUserIdUserWithMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

  // Đếm số lượng chats của user
  long countByUserIdUser(UUID userId);

  // Xóa tất cả chats của user
  void deleteByUserIdUser(UUID userId);
}
