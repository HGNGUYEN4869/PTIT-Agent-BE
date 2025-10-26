package com.agent_chat.agent_chat.DTO;

import java.time.LocalDateTime;

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
  private String idMessage;
  private MessageRole role;
  private String content;
  private LocalDateTime createdAt;
}
