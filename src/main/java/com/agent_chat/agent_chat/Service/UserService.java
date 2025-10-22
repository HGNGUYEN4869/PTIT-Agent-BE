package com.agent_chat.agent_chat.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.agent_chat.agent_chat.Config.JwtUtil;
import com.agent_chat.agent_chat.DTO.AuthResponse;
import com.agent_chat.agent_chat.DTO.LoginRequest;
import com.agent_chat.agent_chat.DTO.RegisterRequest;
import com.agent_chat.agent_chat.Entity.User;
import com.agent_chat.agent_chat.Repository.UserRepository;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtUtil jwtUtil;

  public User register(RegisterRequest request) {
    // kiểm tra email đã tồn tại
    Optional<User> existing = userRepository.findByEmail(request.getEmail());
    if (existing.isPresent()) {
      throw new RuntimeException("Email already exists!");
    }

    // Mã hóa password trước khi lưu
    User user = new User();
    user.setUserName(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setAccessToken(null);
    user.setRefreshToken(null);
    return userRepository.save(user);
  }

    public ResponseEntity<?> getUserResponse(String userId) {
    Optional<User> userOpt = userRepository.findByIdUser(UUID.fromString(userId));
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "User not found 2"));
    }

    User user = userOpt.get();
    return ResponseEntity.ok(Map.of(
        "userId", user.getIdUser(),
        "username", user.getUserName(),
        "email", user.getEmail()));
  }

  public AuthResponse login(LoginRequest loginRequest) {
    Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

    if (userOpt.isEmpty()) {
      throw new RuntimeException("Invalid email or password");
    }

    User user = userOpt.get();

    // Kiểm tra password với BCrypt
    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid username or password");
    }

    // Generate tokens với userId
    String userId = user.getIdUser().toString();
    String accessToken = jwtUtil.generateAccessToken(user.getEmail(), userId);
    String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), userId);

    // Lưu tokens vào database
    user.setAccessToken(accessToken);
    user.setRefreshToken(refreshToken);
    userRepository.save(user);

    return new AuthResponse(
        "Login successful",
        user.getIdUser(),
        user.getUserName(),
        user.getEmail(),
        accessToken,
        refreshToken);
  }

  public AuthResponse refreshToken(String refreshToken) {
    try {
      // Validate refresh token
      String email = jwtUtil.extractEmail(refreshToken);

      if (!jwtUtil.validateToken(refreshToken, email)) {
        throw new RuntimeException("Invalid refresh token");
      }

      // Check token type
      String tokenType = jwtUtil.getTokenType(refreshToken);
      if (!"refresh".equals(tokenType)) {
        throw new RuntimeException("Token is not a refresh token");
      }

      // Find user
      Optional<User> userOpt = userRepository.findByEmail(email);
      if (userOpt.isEmpty()) {
        throw new RuntimeException("User not found");
      }

      User user = userOpt.get();

      // Verify refresh token matches stored token
      if (!refreshToken.equals(user.getRefreshToken())) {
        throw new RuntimeException("Refresh token does not match");
      }

      // Generate new access token với userId
      String userId = user.getIdUser().toString();
      String newAccessToken = jwtUtil.generateAccessToken(email, userId);

      // Update access token in database
      user.setAccessToken(newAccessToken);
      userRepository.save(user);

      return new AuthResponse(
          "Token refreshed successfully",
          user.getIdUser(),
          user.getUserName(),
          user.getEmail(),
          newAccessToken,
          refreshToken);

    } catch (Exception e) {
      throw new RuntimeException("Failed to refresh token: " + e.getMessage());
    }
  }

  public void logout(String email) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      user.setAccessToken(null);
      user.setRefreshToken(null);
      userRepository.save(user);
    }
  }
}
