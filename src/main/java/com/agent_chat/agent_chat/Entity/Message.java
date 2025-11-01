package com.agent_chat.agent_chat.Entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

  private String idMessage;

  private MessageRole role;

  private String content;

  private LocalDateTime createdAt;

  // Enum cho role
  public enum MessageRole {
    USER,
    ASSISTANT
  }
}
