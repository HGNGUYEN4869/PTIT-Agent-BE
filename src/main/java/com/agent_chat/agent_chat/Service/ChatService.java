package com.agent_chat.agent_chat.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agent_chat.agent_chat.DTO.ChatResponse;
import com.agent_chat.agent_chat.DTO.CreateChatRequest;
import com.agent_chat.agent_chat.DTO.CreateMessageRequest;
import com.agent_chat.agent_chat.DTO.MessageDTO;
import com.agent_chat.agent_chat.Entity.Chat;
import com.agent_chat.agent_chat.Entity.Message;
import com.agent_chat.agent_chat.Entity.User;
import com.agent_chat.agent_chat.Repository.ChatRepository;
import com.agent_chat.agent_chat.Repository.MessageRepository;
import com.agent_chat.agent_chat.Repository.UserRepository;

@Service
public class ChatService {

  @Autowired
  private ChatRepository chatRepository;

  @Autowired
  private MessageRepository messageRepository;

  @Autowired
  private UserRepository userRepository;

  // Tạo chat mới
  @Transactional
  public ChatResponse createChat(UUID userId, CreateChatRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Chat chat = Chat.builder()
        .user(user)
        .title(request.getTitle() != null ? request.getTitle() : "New Chat")
        .build();

    chat = chatRepository.save(chat);
    return convertToResponse(chat);
  }

  // Lấy tất cả chats của user
  @Transactional(readOnly = true)
  public List<ChatResponse> getUserChats(UUID userId) {
    List<Chat> chats = chatRepository.findByUserIdUserOrderByUpdatedAtDesc(userId);
    return chats.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  // Lấy chi tiết chat với messages
  @Transactional(readOnly = true)
  public ChatResponse getChatById(UUID chatId, UUID userId) {
    Chat chat = chatRepository.findByIdChatAndUserIdUserWithMessages(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));
    return convertToResponse(chat);
  }

  // Thêm message vào chat
  @Transactional
  public ChatResponse addMessage(UUID chatId, UUID userId, CreateMessageRequest request) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));

    Message message = Message.builder()
        .role(request.getRole())
        .content(request.getContent())
        .chat(chat)
        .build();

    messageRepository.save(message);

    // Cập nhật updatedAt của chat
    chat.setUpdatedAt(LocalDateTime.now());
    chatRepository.save(chat);

    // Reload chat with messages
    return getChatById(chatId, userId);
  }

  // Xóa chat
  @Transactional
  public void deleteChat(UUID chatId, UUID userId) {
    Chat chat = chatRepository.findByIdChatAndUserIdUser(chatId, userId)
        .orElseThrow(() -> new RuntimeException("Chat not found or access denied"));
    chatRepository.delete(chat);
  }

  // Xóa tất cả chats của user
  @Transactional
  public void deleteAllUserChats(UUID userId) {
    chatRepository.deleteByUserIdUser(userId);
  }

  // Cập nhật title của chat
  @Transactional
  public ChatResponse updateChatTitle(UUID chatId, UUID userId, String newTitle) {
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
