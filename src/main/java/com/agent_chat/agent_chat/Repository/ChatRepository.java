package com.agent_chat.agent_chat.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.agent_chat.agent_chat.Entity.Chat;

public interface ChatRepository extends MongoRepository<Chat, String> {

  // Tìm tất cả chats của một user
  List<Chat> findByUserIdUserOrderByUpdatedAtDesc(String userId);

  // Tìm chat theo ID và user ID (để đảm bảo user chỉ truy cập chat của mình)
  Optional<Chat> findByIdChatAndUserIdUser(String chatId, String userId);

  // Đếm số lượng chats của user
  long countByUserIdUser(String userId);

  // Xóa tất cả chats của user
  void deleteByUserIdUser(String userId);
}
