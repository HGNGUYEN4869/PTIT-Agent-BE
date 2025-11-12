package com.agent_chat.agent_chat.WebSocket;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

  // Thay đổi: Hỗ trợ multiple sessions per sessionId
  private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public CompileWebSocketHandler() {
    this.objectMapper = new ObjectMapper();
    // Đăng ký module để hỗ trợ Java 8 Date/Time (LocalDateTime)
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String sessionId = getSessionId(session);

    // Thêm session vào list, hỗ trợ multiple connections
    sessions.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(session);

    System.out.println("✅ WebSocket connected: " + sessionId +
        " (Total connections for this session: " + sessions.get(sessionId).size() + ")");

    // Gửi message chào mừng
    sendLog(sessionId, "Connected to Arduino compile server...", "INFO");
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String sessionId = getSessionId(session);

    // Xóa session cụ thể khỏi list
    List<WebSocketSession> sessionList = sessions.get(sessionId);
    if (sessionList != null) {
      sessionList.remove(session);

      // Nếu không còn connection nào, xóa key
      if (sessionList.isEmpty()) {
        sessions.remove(sessionId);
      }

      int remainingConnections = sessionList.isEmpty() ? 0 : sessionList.size();
      System.out.println("❌ WebSocket disconnected: " + sessionId +
          " (Remaining connections: " + remainingConnections + ")");
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // Có thể handle messages từ client nếu cần
    System.out.println("Received: " + message.getPayload());
  }

  /**
   * Gửi log message tới TẤT CẢ clients đang kết nối với sessionId này
   */
  public void sendLog(String sessionId, String message, String level) {
    List<WebSocketSession> sessionList = sessions.get(sessionId);
    if (sessionList == null || sessionList.isEmpty()) {
      return;
    }

    try {
      CompileLogMessage log = CompileLogMessage.builder()
          .message(message)
          .timestamp(LocalDateTime.now())
          .level(level)
          .build();

      String json = objectMapper.writeValueAsString(log);
      TextMessage textMessage = new TextMessage(json);

      // Broadcast tới tất cả connections
      int successCount = 0;
      int failCount = 0;

      List<WebSocketSession> closedSessions = new ArrayList<>();

      for (WebSocketSession session : sessionList) {
        if (session != null && session.isOpen()) {
          try {
            session.sendMessage(textMessage);
            successCount++;
          } catch (IOException e) {
            System.err.println("Error sending to session: " + e.getMessage());
            closedSessions.add(session);
            failCount++;
          }
        } else {
          closedSessions.add(session);
        }
      }

      // Dọn dẹp các sessions đã đóng
      if (!closedSessions.isEmpty()) {
        sessionList.removeAll(closedSessions);
        if (sessionList.isEmpty()) {
          sessions.remove(sessionId);
        }
      }

      if (successCount > 0) {
        System.out.println("Broadcast log to " + successCount + " client(s) for session: " + sessionId);
      }

    } catch (IOException e) {
      System.err.println("Error creating log message: " + e.getMessage());
    }
  }

  /**
   * Gửi log với level mặc định là INFO
   */
  public void sendLog(String sessionId, String message) {
    sendLog(sessionId, message, "INFO");
  }

  /**
   * Đóng TẤT CẢ connections cho một sessionId
   */
  public void closeSession(String sessionId) {
    List<WebSocketSession> sessionList = sessions.get(sessionId);
    if (sessionList != null) {
      for (WebSocketSession session : sessionList) {
        if (session != null && session.isOpen()) {
          try {
            session.close();
          } catch (IOException e) {
            System.err.println("Error closing session: " + e.getMessage());
          }
        }
      }
      sessions.remove(sessionId);
      System.out.println("Closed all connections for session: " + sessionId);
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
   * Kiểm tra session có ít nhất 1 connection đang hoạt động không
   */
  public boolean hasSession(String sessionId) {
    List<WebSocketSession> sessionList = sessions.get(sessionId);
    if (sessionList == null || sessionList.isEmpty()) {
      return false;
    }

    // Kiểm tra xem có ít nhất 1 session đang mở không
    return sessionList.stream().anyMatch(s -> s != null && s.isOpen());
  }

  /**
   * Lấy số lượng connections đang hoạt động cho một sessionId
   */
  public int getConnectionCount(String sessionId) {
    List<WebSocketSession> sessionList = sessions.get(sessionId);
    if (sessionList == null) {
      return 0;
    }
    return (int) sessionList.stream().filter(s -> s != null && s.isOpen()).count();
  }
}
