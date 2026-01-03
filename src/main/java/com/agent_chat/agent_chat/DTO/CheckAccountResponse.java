package com.agent_chat.agent_chat.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckAccountResponse {
  private String message;
  private String userName;
  private String email;
}
