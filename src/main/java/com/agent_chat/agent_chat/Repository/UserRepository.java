package com.agent_chat.agent_chat.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agent_chat.agent_chat.Entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);
  Optional<User> findByIdUser(UUID idUser);

  Optional<User> findByRefreshToken(String refreshToken);
}
