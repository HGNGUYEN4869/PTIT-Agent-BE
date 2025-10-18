package com.agent_chat.agent_chat.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agent_chat.agent_chat.DTO.AuthResponse;
import com.agent_chat.agent_chat.DTO.LoginRequest;
import com.agent_chat.agent_chat.DTO.RegisterRequest;
import com.agent_chat.agent_chat.Entity.User;
import com.agent_chat.agent_chat.Service.UserService;

@RestController
@RequestMapping("/agent/auth")
public class UserController {

  @Autowired
  private UserService userService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
      User registeredUser = userService.register(request);
      return ResponseEntity.ok(Map.of(
          "message", "User registered successfully",
          "userId", registeredUser.getIdUser(),
          "username", registeredUser.getUserName(),
          "email", registeredUser.getEmail())
          );
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
          .path("/api/auth/refresh") // Chỉ gửi khi call refresh endpoint
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
              "email", authResponse.getEmail()
          ));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
    try {
      if (refreshToken == null || refreshToken.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Refresh token not found in cookie"));
      }

      AuthResponse authResponse = userService.refreshToken(refreshToken);

      // Tạo access token cookie mới
      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(15 * 60)
          .sameSite("Lax")
          .build();

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
          .body(Map.of(
              "message", authResponse.getMessage(),
              "userId", authResponse.getUserId(),
              "username", authResponse.getUserName()));
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

// docker-compose up -d
// docker exec -it mysql_container mysql -uroot -p123456
