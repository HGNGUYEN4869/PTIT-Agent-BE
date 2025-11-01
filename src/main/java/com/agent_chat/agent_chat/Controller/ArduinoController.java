package com.agent_chat.agent_chat.Controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.agent_chat.agent_chat.Config.JwtUtil;
import com.agent_chat.agent_chat.Service.ArduinoCompilerService;

@RestController
@RequestMapping("/h/arduino")
public class ArduinoController {

  @Autowired
  private ArduinoCompilerService compilerService;

  @Autowired
  private JwtUtil jwtUtil;

  /**
   * Upload và compile Arduino file (.ino)
   * 
   * @param file      Arduino sketch file (.ino)
   * @param sessionId WebSocket session ID
   * @param board     Board type (optional, default: arduino:avr:uno)
   * @param token     Access token từ cookie
   * @return Response với sessionId và status
   */
  @PostMapping("/compile")
  public ResponseEntity<?> compileArduino(
      @RequestParam("file") MultipartFile file,
      @RequestParam("sessionId") String sessionId,
      @RequestParam(value = "board", required = false) String board,
      @CookieValue(name = "accessToken", required = false) String token) {

    // Validate access token
    if (token == null || token.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Missing access token"));
    }

    try {
      // Validate token
      String tokenType = jwtUtil.getTokenType(token);
      if (!"access".equals(tokenType)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid token type"));
      }

      // Extract userId (optional - có thể dùng để log)
      String userId = jwtUtil.extractUserId(token);

      // Validate file
      if (file.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "File is empty"));
      }

      String filename = file.getOriginalFilename();
      if (filename == null || !filename.endsWith(".ino")) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Only .ino files are allowed"));
      }

      // Tạo thư mục tạm để lưu sketch
      // Arduino CLI yêu cầu file .ino phải nằm trong folder cùng tên
      String sketchName = filename.replace(".ino", "");
      Path tempDir = Files.createTempDirectory("arduino_" + UUID.randomUUID());
      Path sketchDir = tempDir.resolve(sketchName);
      Files.createDirectories(sketchDir);

      // Lưu file vào thư mục sketch
      File arduinoFile = sketchDir.resolve(filename).toFile();
      try (FileOutputStream fos = new FileOutputStream(arduinoFile)) {
        fos.write(file.getBytes());
      }

      System.out.println("Saved Arduino file: " + arduinoFile.getAbsolutePath());
      System.out.println("User ID: " + userId);
      System.out.println("WebSocket Session: " + sessionId);

      // Set board mặc định nếu không được cung cấp
      String boardType = (board != null && !board.isEmpty()) ? board : "arduino:avr:uno";

      // Compile async để không block request
      CompletableFuture.runAsync(() -> {
        try {
          compilerService.compileArduino(sessionId, arduinoFile, boardType);
        } finally {
          // Cleanup temp files sau khi compile xong
          try {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                  try {
                    Files.delete(path);
                  } catch (IOException e) {
                    // Ignore cleanup errors
                  }
                });
          } catch (IOException e) {
            System.err.println("Failed to cleanup temp files: " + e.getMessage());
          }
        }
      });

      return ResponseEntity.ok(Map.of(
          "message", "Compilation started successfully",
          "sessionId", sessionId,
          "filename", filename,
          "board", boardType));

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Server error: " + e.getMessage()));
    }
  }

  /**
   * Lấy firmware file cho Arduino UNO (.hex)
   * Xóa file ngay sau khi client download xong
   * 
   * @param sessionId WebSocket session ID để compile
   * @param token     Access token từ cookie
   * @return Firmware file .hex
   */
  @GetMapping("/firmware/uno")
  public ResponseEntity<StreamingResponseBody> getUnoFirmware(
      @RequestParam("sessionId") String sessionId,
      @CookieValue(name = "accessToken", required = false) String token) {

    // Validate access token
    if (token == null || token.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      // Validate token
      String tokenType = jwtUtil.getTokenType(token);
      if (!"access".equals(tokenType)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      // Tìm file .hex trong build output của session
      Path buildPath = Paths.get("build", sessionId);

      if (!Files.exists(buildPath)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // Tìm file .hex trong thư mục build
      File hexFile = Files.walk(buildPath)
          .filter(p -> p.toString().endsWith(".hex"))
          .map(Path::toFile)
          .findFirst()
          .orElse(null);

      if (hexFile == null || !hexFile.exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      String filename = hexFile.getName();
      long fileSize = hexFile.length();

      // StreamingResponseBody với cleanup callback
      StreamingResponseBody stream = outputStream -> {
        try (InputStream inputStream = new FileInputStream(hexFile)) {
          byte[] buffer = new byte[8192];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
          outputStream.flush();

          System.out.println(" File streamed successfully: " + filename);

          // Xóa file NGAY sau khi stream xong
          cleanupBuildFiles(buildPath, sessionId);

        } catch (IOException e) {
          System.err.println("Error streaming file: " + e.getMessage());
          // Vẫn cleanup nếu có lỗi
          cleanupBuildFiles(buildPath, sessionId);
          throw e;
        }
      };

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(stream);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Helper method để xóa build files
   */
  private void cleanupBuildFiles(Path buildPath, String sessionId) {
    try {
      Files.walk(buildPath)
          .sorted((a, b) -> b.compareTo(a)) // Xóa file trước, folder sau
          .forEach(path -> {
            try {
              Files.delete(path);
              System.out.println("Deleted: " + path);
            } catch (IOException e) {
              System.err.println("Failed to delete: " + path);
            }
          });
      System.out.println("✅ Cleaned up build files for session: " + sessionId);

      // Cleanup Arduino cache sau khi xóa build files
      compilerService.cleanupArduinoCacheForSession(sessionId);

    } catch (Exception e) {
      System.err.println("Failed to cleanup build files: " + e.getMessage());
    }
  }

  /**
   * Lấy firmware file cho ESP32 (.bin)
   * Xóa file ngay sau khi client download xong
   * 
   * @param sessionId WebSocket session ID để compile
   * @param token     Access token từ cookie
   * @return Firmware file .bin
   */
  @GetMapping("/firmware/esp32")
  public ResponseEntity<StreamingResponseBody> getEsp32Firmware(
      @RequestParam("sessionId") String sessionId,
      @CookieValue(name = "accessToken", required = false) String token) {

    // Validate access token
    if (token == null || token.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      // Validate token
      String tokenType = jwtUtil.getTokenType(token);
      if (!"access".equals(tokenType)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      // Tìm file .bin trong build output của session
      Path buildPath = Paths.get("build", sessionId);

      if (!Files.exists(buildPath)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // Tìm file .bin trong thư mục build
      File binFile = Files.walk(buildPath)
          .filter(p -> p.toString().endsWith(".bin"))
          .map(Path::toFile)
          .findFirst()
          .orElse(null);

      if (binFile == null || !binFile.exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      String filename = binFile.getName();
      long fileSize = binFile.length();

      // StreamingResponseBody với cleanup callback
      StreamingResponseBody stream = outputStream -> {
        try (InputStream inputStream = new FileInputStream(binFile)) {
          byte[] buffer = new byte[8192];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
          outputStream.flush();

          System.out.println(" File streamed successfully: " + filename);

          // Xóa file NGAY sau khi stream xong
          cleanupBuildFiles(buildPath, sessionId);

        } catch (IOException e) {
          System.err.println("Error streaming file: " + e.getMessage());
          // Vẫn cleanup nếu có lỗi
          cleanupBuildFiles(buildPath, sessionId);
          throw e;
        }
      };

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(stream);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Lấy firmware file cho STM32 (.dfu hoặc .bin)
   * Xóa file ngay sau khi client download xong
   * 
   * @param sessionId WebSocket session ID để compile
   * @param token     Access token từ cookie
   * @return Firmware file .dfu hoặc .bin
   */
  @GetMapping("/firmware/stm32")
  public ResponseEntity<StreamingResponseBody> getStm32Firmware(
      @RequestParam("sessionId") String sessionId,
      @CookieValue(name = "accessToken", required = false) String token) {

    // Validate access token
    if (token == null || token.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      // Validate token
      String tokenType = jwtUtil.getTokenType(token);
      if (!"access".equals(tokenType)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      // Tìm file .dfu hoặc .bin trong build output của session
      Path buildPath = Paths.get("build", sessionId);

      if (!Files.exists(buildPath)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      // Tìm file .dfu trước, nếu không có thì tìm .bin
      File firmwareFile = Files.walk(buildPath)
          .filter(p -> p.toString().endsWith(".dfu") || p.toString().endsWith(".bin"))
          .map(Path::toFile)
          .findFirst()
          .orElse(null);

      if (firmwareFile == null || !firmwareFile.exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }

      String filename = firmwareFile.getName();
      long fileSize = firmwareFile.length();

      // StreamingResponseBody với cleanup callback
      StreamingResponseBody stream = outputStream -> {
        try (InputStream inputStream = new FileInputStream(firmwareFile)) {
          byte[] buffer = new byte[8192];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
          outputStream.flush();

          System.out.println(" File streamed successfully: " + filename);

          // Xóa file NGAY sau khi stream xong
          cleanupBuildFiles(buildPath, sessionId);

        } catch (IOException e) {
          System.err.println("Error streaming file: " + e.getMessage());
          // Vẫn cleanup nếu có lỗi
          cleanupBuildFiles(buildPath, sessionId);
          throw e;
        }
      };

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(stream);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Health check endpoint
   */
  @PostMapping("/health")
  public ResponseEntity<?> health() {
    return ResponseEntity.ok(Map.of(
        "status", "OK",
        "service", "Arduino Compiler",
        "timestamp", System.currentTimeMillis()));
  }
}
