package com.agent_chat.agent_chat.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

  @Id
  private String idChat;

  @DBRef
  private User user;

  private String title;

  @Builder.Default
  private List<Message> messages = new ArrayList<>();

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Helper method để thêm message vào chat
  public void addMessage(Message message) {
    messages.add(message);
  }

  // Helper method để xóa message khỏi chat
  public void removeMessage(Message message) {
    messages.remove(message);
  }
}
