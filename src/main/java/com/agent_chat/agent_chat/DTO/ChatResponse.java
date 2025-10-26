package com.agent_chat.agent_chat.DTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
  private String idChat;
  private String idUser;
  private String title;
  @Builder.Default
  private List<MessageDTO> messages = new ArrayList<>();
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private int messageCount;
}
