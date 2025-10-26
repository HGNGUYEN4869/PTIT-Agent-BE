package com.agent_chat.agent_chat.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agent_chat.agent_chat.DTO.ChatResponse;
import com.agent_chat.agent_chat.DTO.CreateChatRequest;
import com.agent_chat.agent_chat.DTO.CreateMessageRequest;
import com.agent_chat.agent_chat.DTO.MessageDTO;
import com.agent_chat.agent_chat.Entity.Chat;
import com.agent_chat.agent_chat.Entity.Message;
import com.agent_chat.agent_chat.Entity.User;
import com.agent_chat.agent_chat.Repository.ChatRepository;
import com.agent_chat.agent_chat.Repository.UserRepository;

@Service
public class ChatService {

  @Autowired
  private ChatRepository chatRepository;

  @Autowired
  private UserRepository userRepository;

  // Tạo chat mới
  public ChatResponse createChat(String userId, CreateChatRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Chat chat = Chat.builder()
        .user(user)
        .title(request.getTitle() != null ? request.getTitle() : "New Chat")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    chat = chatRepository.save(chat);
    return convertToResponse(chat);
  }

  // Lấy tất cả chats của user
  public List<ChatResponse> getUserChats(String userId) {
    List<Chat> chats = chatRepository.findByUserIdUserOrderByUpdatedAtDesc(userId);
    return chats.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  // Lấy chi tiết chat với messages
  public ChatResponse getChatById(String chatId, String userId) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));
    return convertToResponse(chat);
  }

  // Thêm message vào chat
  public ChatResponse addMessage(String chatId, String userId, CreateMessageRequest request) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));

    Message message = Message.builder()
        .idMessage(UUID.randomUUID().toString())
        .role(request.getRole())
        .content(request.getContent())
        .createdAt(LocalDateTime.now())
        .build();

    chat.addMessage(message);
    chat.setUpdatedAt(LocalDateTime.now());
    chatRepository.save(chat);

    return getChatById(chatId, userId);
  }

  // Xóa chat
  public void deleteChat(String chatId, String userId) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));
    chatRepository.delete(chat);
  }

  // Xóa tất cả chats của user
  public void deleteAllUserChats(String userId) {
    chatRepository.deleteByUserIdUser(userId);
  }

  // Cập nhật title của chat
  public ChatResponse updateChatTitle(String chatId, String userId, String newTitle) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));

    chat.setTitle(newTitle);
    chat = chatRepository.save(chat);

    return convertToResponse(chat);
  }

  // Helper method: Convert Entity to DTO
  private ChatResponse convertToResponse(Chat chat) {
    return ChatResponse.builder()
        .idChat(chat.getIdChat())
        .idUser(chat.getUser().getIdUser())
        .title(chat.getTitle())
        .messages(chat.getMessages().stream()
            .map(msg -> MessageDTO.builder()
                .idMessage(msg.getIdMessage())
                .role(msg.getRole())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build())
            .collect(Collectors.toList()))
        .createdAt(chat.getCreatedAt())
        .updatedAt(chat.getUpdatedAt())
        .messageCount(chat.getMessages().size())
        .build();
  }
}
