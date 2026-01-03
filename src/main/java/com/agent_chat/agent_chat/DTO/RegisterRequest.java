package com.agent_chat.agent_chat.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class RegisterRequest {
    private String username;
    private String password;
    private String stuId;
    private String citizenId;
    private String email;
}
