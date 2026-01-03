package com.agent_chat.agent_chat.Service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.agent_chat.agent_chat.Config.JwtUtil;
import com.agent_chat.agent_chat.DTO.AuthResponse;
import com.agent_chat.agent_chat.DTO.CheckAccount;
import com.agent_chat.agent_chat.DTO.CheckAccountResponse;
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

  public AuthResponse register(RegisterRequest request) {
    // Kiểm tra nếu cả stuId và citizenId đều null hoặc ""
    boolean stuIdEmpty = request.getStuId() == null || request.getStuId().isEmpty();
    boolean citizenIdEmpty = request.getCitizenId() == null || request.getCitizenId().isEmpty();

    if (stuIdEmpty && citizenIdEmpty) {
      throw new RuntimeException("Chưa đăng ký căn cước hoặc mã sv");
    }

    // Kiểm tra stuId khác "" trước
    if (request.getStuId() != null && !request.getStuId().isEmpty()) {
      Optional<User> existingStuId = userRepository.findByStuId(request.getStuId());
      if (existingStuId.isPresent()) {
        throw new RuntimeException("Tài khoản có mã sv đã tồn tại");
      }
    }

    // Kiểm tra citizenId khác "" trước
    if (request.getCitizenId() != null && !request.getCitizenId().isEmpty()) {
      Optional<User> existingCitizenId = userRepository.findByCitizenId(request.getCitizenId());
      if (existingCitizenId.isPresent()) {
        throw new RuntimeException("Tài khoản có mã căn cước đã tồn tại");
      }
    }

    // kiểm tra email đã tồn tại
    Optional<User> existing = userRepository.findByEmail(request.getEmail());
    if (existing.isPresent()) {
      throw new RuntimeException("Email already exists!");
    }

    // Mã hóa password trước khi lưu
    User user = new User();
    user.setUserName(request.getUsername());
    user.setEmail(request.getEmail());
    user.setStuId(request.getStuId());
    user.setCitizenId(request.getCitizenId());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    // Tạo tokens ngay sau khi register (auto-login)
    user = userRepository.save(user); // Save để có ID

    String userId = user.getIdUser();
    String accessToken = jwtUtil.generateAccessToken(user.getEmail(), userId);
    String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), userId);

    // Lưu tokens vào database
    user.setAccessToken(accessToken);
    user.setRefreshToken(refreshToken);
    userRepository.save(user);

    return new AuthResponse(
        "Registration successful",
        user.getIdUser(),
        user.getUserName(),
        user.getStuId() != null ? user.getStuId() : "",
        user.getCitizenId() != null ? user.getCitizenId() : "",
        user.getEmail(),
        accessToken,
        refreshToken);
  }

  public ResponseEntity<?> getUserResponse(String userId) {
    Optional<User> userOpt = userRepository.findByIdUser(userId);
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "User not found 2"));
    }

    User user = userOpt.get();
    return ResponseEntity.ok(Map.of(
        "userId", user.getIdUser(),
        "username", user.getUserName(),
        "stuId", user.getStuId() != null ? user.getStuId() : "",
        "citizenId", user.getCitizenId() != null ? user.getCitizenId() : "",
        "email", user.getEmail()));
  }

  public AuthResponse login(LoginRequest loginRequest) {
    Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

    if (userOpt.isEmpty()) {
      throw new RuntimeException("Email không tồn tại");
    }

    User user = userOpt.get();

    // Kiểm tra password với BCrypt
    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      throw new RuntimeException("Sai mật khẩu");
    }

    // Generate tokens với userId
    String userId = user.getIdUser();
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
        user.getStuId() != null ? user.getStuId() : "",
        user.getCitizenId() != null ? user.getCitizenId() : "",
        user.getEmail(),
        accessToken,
        refreshToken);
  }

  public CheckAccountResponse checkAccount(CheckAccount checkAccount) {
    if (checkAccount.getStuId() == "" && checkAccount.getCitizenId() == "") {
      throw new RuntimeException("Lỗi không có request");
    }

    Optional<User> userOpt = userRepository.findByStuId(checkAccount.getStuId());

    if (userOpt.isEmpty()) {
      userOpt = userRepository.findByCitizenId(checkAccount.getCitizenId());
    }

    if (userOpt.isEmpty()) {
      return new CheckAccountResponse(
          "Không tồn tại tài khoản",
          "",
          "");
    }

    User user = userOpt.get();

    return new CheckAccountResponse(
        "Tồn tại tài khoản",
        user.getUserName(),
        user.getEmail());
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
      String userId = user.getIdUser();
      String newAccessToken = jwtUtil.generateAccessToken(email, userId);

      // Update access token in database
      user.setAccessToken(newAccessToken);
      userRepository.save(user);

      return new AuthResponse(
          "Token refreshed successfully",
          user.getIdUser(),
          user.getUserName(),
          user.getStuId() != null ? user.getStuId() : "",
          user.getCitizenId() != null ? user.getCitizenId() : "",
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
