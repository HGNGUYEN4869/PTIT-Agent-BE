package com.agent_chat.agent_chat.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.agent_chat.agent_chat.WebSocket.CompileWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final CompileWebSocketHandler compileHandler;

  public WebSocketConfig(CompileWebSocketHandler compileHandler) {
    this.compileHandler = compileHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(compileHandler, "/ws/compile/{sessionId}")
        .setAllowedOrigins("*"); // Allow all origins (dev mode - đổi lại trong production!)
  }
}
