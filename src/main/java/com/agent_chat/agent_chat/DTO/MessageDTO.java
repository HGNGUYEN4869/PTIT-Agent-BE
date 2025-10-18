package com.agent_chat.agent_chat.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.agent_chat.agent_chat.Entity.Message.MessageRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
  private UUID idMessage;
  private MessageRole role;
  private String content;
  private LocalDateTime createdAt;
}
