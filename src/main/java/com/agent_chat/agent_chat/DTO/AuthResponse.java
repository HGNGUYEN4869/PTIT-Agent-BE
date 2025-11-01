package com.agent_chat.agent_chat.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  private String message;
  private String userId;
  private String userName;
  private String email;
  private String accessToken;
  private String refreshToken;
}
