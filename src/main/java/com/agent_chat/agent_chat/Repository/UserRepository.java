package com.agent_chat.agent_chat.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.agent_chat.agent_chat.Entity.User;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByEmail(String email);
  Optional<User> findByStuId(String stuId);
  Optional<User> findByCitizenId(String citizenId);
  Optional<User> findByIdUser(String idUser);

  Optional<User> findByRefreshToken(String refreshToken);
}
