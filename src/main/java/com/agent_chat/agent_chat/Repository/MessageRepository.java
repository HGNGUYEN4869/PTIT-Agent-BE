package com.agent_chat.agent_chat.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.agent_chat.agent_chat.Entity.Message;

// MessageRepository không còn cần thiết vì Message đã embedded trong Chat
// Nếu cần query messages, query qua ChatRepository
public interface MessageRepository extends MongoRepository<Message, String> {
    // Không có methods - chỉ giữ interface để tránh break code
    // TODO: Remove this repository sau khi clean up code
}
