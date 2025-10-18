package com.agent_chat.agent_chat.DTO;

import com.agent_chat.agent_chat.Entity.Message.MessageRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequest {
  private MessageRole role;
  private String content;
}
