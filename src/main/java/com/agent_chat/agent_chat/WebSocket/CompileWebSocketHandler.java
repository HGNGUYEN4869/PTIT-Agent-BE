package com.agent_chat.agent_chat.WebSocket;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.agent_chat.agent_chat.DTO.CompileLogMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class CompileWebSocketHandler extends TextWebSocketHandler {

  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public CompileWebSocketHandler() {
    this.objectMapper = new ObjectMapper();
    // Đăng ký module để hỗ trợ Java 8 Date/Time (LocalDateTime)
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String sessionId = getSessionId(session);
    sessions.put(sessionId, session);
    System.out.println(" WebSocket connected: " + sessionId);

    // Gửi message chào mừng
    sendLog(sessionId, "Connected to Arduino compile server...", "INFO");
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String sessionId = getSessionId(session);
    sessions.remove(sessionId);
    System.out.println("WebSocket disconnected: " + sessionId);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // Có thể handle messages từ client nếu cần
    System.out.println("Received: " + message.getPayload());
  }

  /**
   * Gửi log message tới client qua WebSocket
   */
  public void sendLog(String sessionId, String message, String level) {
    WebSocketSession session = sessions.get(sessionId);
    if (session != null && session.isOpen()) {
      try {
        CompileLogMessage log = CompileLogMessage.builder()
            .message(message)
            .timestamp(LocalDateTime.now())
            .level(level)
            .build();

        String json = objectMapper.writeValueAsString(log);
        session.sendMessage(new TextMessage(json));

      } catch (IOException e) {
        System.err.println("Error sending log: " + e.getMessage());
      }
    }
  }

  /**
   * Gửi log với level mặc định là INFO
   */
  public void sendLog(String sessionId, String message) {
    sendLog(sessionId, message, "INFO");
  }

  /**
   * Đóng connection
   */
  public void closeSession(String sessionId) {
    WebSocketSession session = sessions.get(sessionId);
    if (session != null && session.isOpen()) {
      try {
        session.close();
        sessions.remove(sessionId);
      } catch (IOException e) {
        System.err.println("Error closing session: " + e.getMessage());
      }
    }
  }

  /**
   * Extract sessionId từ WebSocket URI
   * URI format: /ws/compile/{sessionId}
   */
  private String getSessionId(WebSocketSession session) {
    if (session == null) {
      return "unknown-session";
    }

    var uri = session.getUri();
    if (uri == null) {
      return "unknown-session";
    }

    String path = uri.getPath();
    if (path == null || path.isEmpty()) {
      return "unknown-session";
    }

    // Extract phần cuối cùng của path
    String[] parts = path.split("/");
    if (parts.length > 0) {
      return parts[parts.length - 1];
    }

    return "unknown-session";
  }

  /**
   * Kiểm tra session có tồn tại không
   */
  public boolean hasSession(String sessionId) {
    return sessions.containsKey(sessionId) && sessions.get(sessionId).isOpen();
  }
}
