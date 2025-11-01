package com.agent_chat.agent_chat.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompileLogMessage {
  private String message;
  private LocalDateTime timestamp;
  private String level; // INFO, ERROR, SUCCESS, WARNING
}
