package com.agent_chat.agent_chat.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agent_chat.agent_chat.Config.JwtUtil;
import com.agent_chat.agent_chat.DTO.ChatResponse;
import com.agent_chat.agent_chat.DTO.CreateChatRequest;
import com.agent_chat.agent_chat.DTO.CreateMessageRequest;
import com.agent_chat.agent_chat.Service.ChatService;

@RestController
@RequestMapping("/h/chats")
public class ChatController {

  @Autowired
  private ChatService chatService;

  @Autowired
  private JwtUtil jwtUtil;

  // Extract userId từ accessToken cookie
  private UUID extractUserIdFromToken(String token) {
    try {
      // Validate token trước
      // String email = jwtUtil.extractEmail(token);
      // if (!jwtUtil.validateToken(token, email)) {
      //   throw new RuntimeException("Invalid or expired token");
      // }

      // Check token type
      String tokenType = jwtUtil.getTokenType(token);
      if (!"access".equals(tokenType)) {
        throw new RuntimeException("Invalid token type");
      }

      // Extract userId
      String userId = jwtUtil.extractUserId(token);
      return UUID.fromString(userId);
    } catch (Exception e) {
      throw new RuntimeException("Authentication failed: " + e.getMessage());
    }
  }

  // Tạo chat mới
  @PostMapping("/createChat")
  public ResponseEntity<?> createChat(
      @CookieValue("accessToken") String token,
      @RequestBody CreateChatRequest request) {
    try {
      UUID userId = extractUserIdFromToken(token);
      ChatResponse response = chatService.createChat(userId, request);
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Lấy tất cả chats của user
  @GetMapping("/user")
  public ResponseEntity<?> getUserChats(@CookieValue("accessToken") String token) {
    try {
      UUID userId = extractUserIdFromToken(token);
      List<ChatResponse> chats = chatService.getUserChats(userId);
      return ResponseEntity.ok(chats);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Lấy chi tiết chat với messages
  @GetMapping("/{chatId}")
  public ResponseEntity<?> getChatById(
      @PathVariable UUID chatId,
      @CookieValue("accessToken") String token) {
    try {
      UUID userId = extractUserIdFromToken(token);
      ChatResponse chat = chatService.getChatById(chatId, userId);
      return ResponseEntity.ok(chat);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Thêm message vào chat
  @PostMapping("/{chatId}/messages")
  public ResponseEntity<?> addMessage(
      @PathVariable UUID chatId,
      @CookieValue("accessToken") String token,
      @RequestBody CreateMessageRequest request) {
    try {
      UUID userId = extractUserIdFromToken(token);
      chatService.addMessage(chatId, userId, request);
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Cập nhật title của chat
  @PutMapping("/{chatId}/title")
  public ResponseEntity<?> updateChatTitle(
      @PathVariable UUID chatId,
      @CookieValue("accessToken") String token,
      @RequestBody Map<String, String> body) {
    try {
      UUID userId = extractUserIdFromToken(token);
      String newTitle = body.get("title");
      ChatResponse response = chatService.updateChatTitle(chatId, userId, newTitle);
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Xóa chat
  @DeleteMapping("/{chatId}")
  public ResponseEntity<?> deleteChat(
      @PathVariable UUID chatId,
      @CookieValue("accessToken") String token) {
    try {
      UUID userId = extractUserIdFromToken(token);
      chatService.deleteChat(chatId, userId);
      return ResponseEntity.ok(Map.of("message", "Chat deleted successfully"));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // Xóa tất cả chats của user
  @DeleteMapping
  public ResponseEntity<?> deleteAllUserChats(@CookieValue("accessToken") String token) {
    try {
      UUID userId = extractUserIdFromToken(token);
      chatService.deleteAllUserChats(userId);
      return ResponseEntity.ok(Map.of("message", "All chats deleted successfully"));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
