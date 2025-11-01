package com.agent_chat.agent_chat.Controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agent_chat.agent_chat.Config.JwtUtil;
import com.agent_chat.agent_chat.DTO.AuthResponse;
import com.agent_chat.agent_chat.DTO.LoginRequest;
import com.agent_chat.agent_chat.DTO.RegisterRequest;
import com.agent_chat.agent_chat.Entity.User;
import com.agent_chat.agent_chat.Repository.UserRepository;
import com.agent_chat.agent_chat.Service.UserService;

@RestController
@RequestMapping("/agent/auth")
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private UserRepository userRepository;

  /**
   * GET /agent/auth/me
   * Lấy thông tin user hiện tại từ accessToken cookie.
   * Nếu accessToken hết hạn, tự động dùng refreshToken để tạo token mới.
   * 
   * @param accessToken  - HttpOnly cookie chứa JWT access token
   * @param refreshToken - HttpOnly cookie chứa JWT refresh token
   * @return Thông tin user (không bao gồm password và tokens)
   */
  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(
      @CookieValue(name = "accessToken", required = false) String accessToken,
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    try {
      // 1. Thử xử lý access token
      ResponseEntity<?> accessResponse = handleAccessToken(accessToken);
      if (accessResponse != null) {
        return accessResponse;
      }
      // 2. Nếu access token không hợp lệ, dùng refresh token
      return handleRefreshToken(refreshToken);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Server error: " + e.getMessage()));
    }
  }

  // Xử lý access token
  private ResponseEntity<?> handleAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isEmpty())
      return null;

    try {
      String userId = jwtUtil.extractUserId(accessToken);
      if (jwtUtil.validateToken(accessToken, userId)) {
        return userService.getUserResponse(userId);
      }
    } catch (Exception ignored) {
      // Token không hợp lệ hoặc hết hạn, trả về null để xử lý refresh
    }
    return null;
  }

  // Xử lý refresh token
  private ResponseEntity<?> handleRefreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "No valid tokens found. Please login again."));
    }

    try {
      String userId = jwtUtil.extractUserId(refreshToken);

      if (!jwtUtil.validateToken(refreshToken, userId)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Refresh token expired. Please login again."));
      }

      if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid token type"));
      }

      Optional<User> userOpt = userRepository.findByIdUser(userId);
      if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "User not found"));
      }

      User user = userOpt.get();
      if (!refreshToken.equals(user.getRefreshToken())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid refresh token"));
      }

      // Tạo access token mới
      String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), userId);
      user.setAccessToken(newAccessToken);
      userRepository.save(user);

      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(15 * 60)
          .sameSite("Lax")
          .build();

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
          .body(Map.of(
              "userId", user.getIdUser(),
              "username", user.getUserName(),
              "email", user.getEmail(),
              "tokenRefreshed", true));

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
      AuthResponse authResponse = userService.register(request);

      // Set Access Token as HttpOnly Cookie (15 minutes)
      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(15 * 60)
          .sameSite("Lax")
          .build();

      // Set Refresh Token as HttpOnly Cookie (7 days)
      ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
          .httpOnly(true)
          .secure(false)
          .path("/agent/auth/me")
          .maxAge(7 * 24 * 60 * 60)
          .sameSite("Lax")
          .build();

      // Trả về response với cookies và thông tin user
      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
          .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
          .body(Map.of(
              "message", authResponse.getMessage(),
              "userId", authResponse.getUserId(),
              "username", authResponse.getUserName(),
              "email", authResponse.getEmail()));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    try {
      AuthResponse authResponse = userService.login(loginRequest);

      // Set Access Token as HttpOnly Cookie (15 minutes)
      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
          .httpOnly(true) // Không thể đọc bằng JavaScript
          .secure(false) // Set true khi dùng HTTPS trong production
          .path("/") // Cookie có hiệu lực cho toàn bộ domain
          .maxAge(15 * 60) // 15 phút (giống ACCESS_TOKEN_VALIDITY)
          .sameSite("Lax") // Chống CSRF attack
          .build();

      // Set Refresh Token as HttpOnly Cookie (7 days)
      ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
          .httpOnly(true) // Không thể đọc bằng JavaScript
          .secure(false) // Set true khi dùng HTTPS trong production
          .path("/agent/auth/me") // Chỉ gửi khi call refresh endpoint
          .maxAge(7 * 24 * 60 * 60) // 7 ngày (giống REFRESH_TOKEN_VALIDITY)
          .sameSite("Lax") // Chống CSRF attack
          .build();

      // Trả về response với cookies trong header và KHÔNG chứa tokens trong body
      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
          .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
          .body(Map.of(
              "message", authResponse.getMessage(),
              "userId", authResponse.getUserId(),
              "username", authResponse.getUserName(),
              "email", authResponse.getEmail()));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @CookieValue(name = "accessToken", required = false) String accessToken,
      @RequestBody Map<String, String> body) {
    try {
      String email = body.get("email");
      userService.logout(email);

      // Xóa cookies bằng cách set maxAge = 0
      ResponseCookie deleteAccessToken = ResponseCookie.from("accessToken", "")
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(0)
          .build();

      ResponseCookie deleteRefreshToken = ResponseCookie.from("refreshToken", "")
          .httpOnly(true)
          .secure(false)
          .path("/api/auth/refresh")
          .maxAge(0)
          .build();

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, deleteAccessToken.toString())
          .header(HttpHeaders.SET_COOKIE, deleteRefreshToken.toString())
          .body(Map.of("message", "Logout successful"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
