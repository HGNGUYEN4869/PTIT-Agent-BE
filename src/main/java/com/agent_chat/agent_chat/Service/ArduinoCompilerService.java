package com.agent_chat.agent_chat.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.agent_chat.agent_chat.WebSocket.CompileWebSocketHandler;

@Service
public class ArduinoCompilerService {

  private final CompileWebSocketHandler wsHandler;
  private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

  // Track sketch names by sessionId for cleanup
  private final java.util.Map<String, String> sessionSketchNames = new java.util.concurrent.ConcurrentHashMap<>();

  public ArduinoCompilerService(CompileWebSocketHandler wsHandler) {
    this.wsHandler = wsHandler;
  }

  /**
   * Compile Arduino code và stream logs real-time qua WebSocket
   * 
   * @param sessionId   WebSocket session ID
   * @param arduinoFile File .ino cần compile
   * @param board       Board type (vd: "arduino:avr:uno")
   */
  public void compileArduino(String sessionId, File arduinoFile, String board) {
    String timestamp = LocalDateTime.now().format(timeFormatter);

    try {
      // Kiểm tra WebSocket session
      if (!wsHandler.hasSession(sessionId)) {
        System.err.println("WebSocket session not found: " + sessionId);
        return;
      }

      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "INFO");
      wsHandler.sendLog(sessionId, "Starting Arduino compilation...", "INFO");
      wsHandler.sendLog(sessionId, "File: " + arduinoFile.getName(), "INFO");
      wsHandler.sendLog(sessionId, "Board: " + board, "INFO");
      wsHandler.sendLog(sessionId, "Time: " + timestamp, "INFO");
      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "INFO");
      wsHandler.sendLog(sessionId, "", "INFO");

      // Kiểm tra file tồn tại
      if (!arduinoFile.exists()) {
        wsHandler.sendLog(sessionId, "File not found: " + arduinoFile.getAbsolutePath(), "ERROR");
        wsHandler.sendLog(sessionId, "Compilation aborted!", "ERROR");
        return;
      }

      // Path oldBuildPath = Paths.get("build", sessionId);
      // if (Files.exists(oldBuildPath)) {
      //   cleanupOldBuildFiles(oldBuildPath, sessionId);
      // }

      // cleanupArduinoCacheForSession(sessionId);

      // On-demand: Đảm bảo board đã được cài đặt
      ensureBoardInstalled(sessionId, board);

      // On-demand: Đảm bảo libraries đã được cài đặt
      ensureLibrariesInstalled(sessionId, arduinoFile);

      // Build arduino-cli command
      ProcessBuilder pb = new ProcessBuilder(
          "arduino-cli", "compile",
          "--fqbn", board,
          "--verbose",
          arduinoFile.getParentFile().getAbsolutePath() // Path to sketch folder
      );

      pb.redirectErrorStream(true); // Merge stderr vào stdout

      wsHandler.sendLog(sessionId, "Running: arduino-cli compile --fqbn " + board, "INFO");
      wsHandler.sendLog(sessionId, "", "INFO");

      // Start process
      Process process = pb.start();

      // Stream output real-time
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {

        String line;
        while ((line = reader.readLine()) != null) {
          // Phân loại log level dựa vào nội dung
          String level = detectLogLevel(line);
          wsHandler.sendLog(sessionId, line, level);
        }
      }

      // Wait for process to complete
      int exitCode = process.waitFor();

      wsHandler.sendLog(sessionId, "", "INFO");
      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "INFO");

      if (exitCode == 0) {
        wsHandler.sendLog(sessionId, " Compilation successful!", "SUCCESS");

        // Copy compiled firmware files to build directory for download
        try {
          copyCompiledFirmware(sessionId, arduinoFile, board);
          wsHandler.sendLog(sessionId, " Firmware files saved for download", "SUCCESS");
        } catch (Exception e) {
          wsHandler.sendLog(sessionId, "Warning: Failed to save firmware files: " + e.getMessage(), "WARNING");
        }

        wsHandler.sendLog(sessionId, "Your Arduino code is ready to upload!", "SUCCESS");
      } else {
        wsHandler.sendLog(sessionId, "Compilation failed!", "ERROR");
        wsHandler.sendLog(sessionId, "Exit code: " + exitCode, "ERROR");
        wsHandler.sendLog(sessionId, "Check the errors above for details", "WARNING");
      }

      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "INFO");

    } catch (Exception e) {
      wsHandler.sendLog(sessionId, "", "INFO");
      wsHandler.sendLog(sessionId, "Compilation error: " + e.getMessage(), "ERROR");
      wsHandler.sendLog(sessionId, "Stack trace:", "ERROR");

      for (StackTraceElement element : e.getStackTrace()) {
        wsHandler.sendLog(sessionId, "   " + element.toString(), "ERROR");
      }

      e.printStackTrace();
    }
  }

  /**
   * Phát hiện log level từ nội dung message
   */
  private String detectLogLevel(String line) {
    String lower = line.toLowerCase();

    if (lower.contains("error") || lower.contains("failed") || lower.contains("fatal")) {
      return "ERROR";
    } else if (lower.contains("warning") || lower.contains("warn")) {
      return "WARNING";
    } else if (lower.contains("success") || lower.contains("done") || lower.contains("completed")) {
      return "SUCCESS";
    } else {
      return "INFO";
    }
  }

  /**
   * Compile với board mặc định (Arduino Uno)
   */
  public void compileArduino(String sessionId, File arduinoFile) {
    compileArduino(sessionId, arduinoFile, "arduino:avr:uno");
  }

  /**
   * Copy compiled firmware files (hex, bin, dfu) to build directory for download
   * 
   * @param sessionId   WebSocket session ID
   * @param arduinoFile Original Arduino sketch file
   * @param board       Board type used for compilation
   */
  private void copyCompiledFirmware(String sessionId, File arduinoFile, String board) throws Exception {
    // Create build directory for this session
    Path buildDir = Paths.get("build", sessionId);
    Files.createDirectories(buildDir);

    // Arduino CLI stores build output in cache:
    // /root/.cache/arduino/sketches/<HASH>/
    // Try to find it by searching for files with same name as .ino file
    String sketchName = arduinoFile.getName().replace(".ino", "");

    // Lưu sketch name để cleanup sau này
    sessionSketchNames.put(sessionId, sketchName);
    System.out.println("Tracked sketch name for session " + sessionId + ": " + sketchName);

    // Search in common cache locations
    String[] cachePaths = {
        System.getProperty("user.home") + "/.cache/arduino/sketches",
        System.getProperty("user.home") + "/.arduino15/sketches",
        "/tmp" // Fallback to temp folder
    };

    File buildFolder = null;
    for (String cachePath : cachePaths) {
      File cacheDir = new File(cachePath);
      if (cacheDir.exists()) {
        buildFolder = cacheDir;
        System.out.println("Searching Arduino cache: " + cachePath);
        break;
      }
    }

    if (buildFolder == null) {
      // Fallback: search in sketch folder and parent
      buildFolder = arduinoFile.getParentFile();
      System.out.println("Arduino cache not found, searching sketch folder: " + buildFolder.getAbsolutePath());
    }

    // Track copied files để tránh duplicate logs
    final java.util.Set<String> copiedFiles = new java.util.HashSet<>();

    // Search and copy all firmware files matching sketch name
    final File searchFolder = buildFolder;
    boolean foundFiles = Files.walk(searchFolder.toPath(), 10) // Search max 10 levels deep
        .filter(Files::isRegularFile)
        .filter(p -> {
          String fileName = p.getFileName().toString();
          String nameLower = fileName.toLowerCase();
          // Match files with sketch name and firmware extensions
          return (fileName.startsWith(sketchName) || fileName.contains(sketchName)) &&
              (nameLower.endsWith(".hex") ||
                  nameLower.endsWith(".bin") ||
                  nameLower.endsWith(".elf") ||
                  nameLower.endsWith(".dfu"));
        })
        .peek(sourcePath -> {
          try {
            String fileName = sourcePath.getFileName().toString();
            // Chỉ copy và log nếu chưa copy file này
            if (!copiedFiles.contains(fileName)) {
              Path targetPath = buildDir.resolve(fileName);
              Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
              copiedFiles.add(fileName);
              System.out.println("Copied firmware: " + fileName + " -> " + targetPath);
              wsHandler.sendLog(sessionId, "Saved: " + fileName, "SUCCESS");
            }
          } catch (Exception e) {
            System.err.println("Failed to copy " + sourcePath.getFileName() + ": " + e.getMessage());
          }
        })
        .count() > 0;

    if (!foundFiles) {
      wsHandler.sendLog(sessionId, "No firmware files (.hex, .bin) found!", "WARNING");
      wsHandler.sendLog(sessionId, "Searched in: " + searchFolder.getAbsolutePath(), "WARNING");
    }
  }

  /**
   * Cleanup Arduino cache directory cho một session cụ thể
   * Được gọi từ Controller sau khi user download firmware xong
   * 
   * @param sessionId WebSocket session ID
   */
  public void cleanupArduinoCacheForSession(String sessionId) {
    try {
      // Lấy sketch name từ map
      String sketchName = sessionSketchNames.get(sessionId);
      if (sketchName == null) {
        System.out.println("No sketch name found for session: " + sessionId);
        return;
      }

      System.out.println("Cleaning up Arduino cache for session: " + sessionId + " (sketch: " + sketchName + ")");

      // Search in common cache locations
      String[] cachePaths = {
          System.getProperty("user.home") + "/.cache/arduino/sketches",
          System.getProperty("user.home") + "/.arduino15/sketches"
      };

      final java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);

      for (String cachePath : cachePaths) {
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists()) {
          System.out.println("Cache directory not found: " + cachePath);
          continue;
        }

        System.out.println("Searching in: " + cachePath);

        // Tìm và xóa tất cả files matching sketch name
        Files.walk(Paths.get(cachePath), 10)
            .filter(Files::isRegularFile)
            .filter(p -> {
              String fileName = p.getFileName().toString();
              // Match files với sketch name và firmware extensions
              return (fileName.startsWith(sketchName) || fileName.contains(sketchName)) &&
                  (fileName.endsWith(".hex") ||
                      fileName.endsWith(".bin") ||
                      fileName.endsWith(".elf") ||
                      fileName.endsWith(".eep") ||
                      fileName.endsWith(".dfu"));
            })
            .forEach(filePath -> {
              try {
                Files.delete(filePath);
                deletedCount.incrementAndGet();
                System.out.println("Deleted Arduino cache file: " + filePath);
              } catch (Exception e) {
                System.err.println("Failed to delete " + filePath + ": " + e.getMessage());
              }
            });

        // Xóa các empty directories sau khi xóa files
        Files.walk(Paths.get(cachePath), 10)
            .filter(Files::isDirectory)
            .sorted((a, b) -> b.toString().length() - a.toString().length()) // Deeper first
            .forEach(dir -> {
              try {
                // Chỉ xóa nếu directory rỗng
                if (Files.list(dir).count() == 0) {
                  Files.delete(dir);
                  System.out.println("Deleted empty directory: " + dir);
                  deletedCount.incrementAndGet();
                }
              } catch (Exception e) {
                // Ignore errors (directory may not be empty or permission denied)
              }
            });
      }

      System.out.println("Arduino cache cleanup completed - deleted " + deletedCount.get() + " items");

      // Remove từ map sau khi cleanup xong
      sessionSketchNames.remove(sessionId);

    } catch (Exception e) {
      System.err.println("Failed to cleanup Arduino cache: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * On-demand: Đảm bảo board đã được cài đặt
   * Tự động cài board nếu chưa có, với progress thông báo qua WebSocket
   */
  private void ensureBoardInstalled(String sessionId, String boardFqbn) {
    try {
      // Parse board FQBN: "esp32:esp32:esp32" → core = "esp32:esp32"
      String[] parts = boardFqbn.split(":");
      if (parts.length < 2) {
        wsHandler.sendLog(sessionId, "Invalid board format: " + boardFqbn, "WARN");
        return;
      }

      String core = parts[0] + ":" + parts[1];

      // Check nếu board đã được cài
      if (isCoreInstalled(core)) {
        wsHandler.sendLog(sessionId, "Board " + core + " đã có sẵn", "INFO");
        return;
      }

      // Board chưa có → cài mới
      wsHandler.sendLog(sessionId, "", "INFO");
      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "WARN");
      wsHandler.sendLog(sessionId, "Board " + core + " chưa có, đang cài...", "WARN");
      wsHandler.sendLog(sessionId, "Quá trình này có thể mất 2-5 phút", "WARN");
      wsHandler.sendLog(sessionId, "═══════════════════════════════════════", "WARN");
      wsHandler.sendLog(sessionId, "", "INFO");

      // Get additional URL cho board
      String additionalUrl = getBoardPackageUrl(core);

      // Update core index trước
      wsHandler.sendLog(sessionId, "Đang cập nhật board index...", "INFO");
      executeCommand(new ProcessBuilder("arduino-cli", "core", "update-index"));

      // Install core
      ProcessBuilder installCmd;
      if (additionalUrl != null) {
        installCmd = new ProcessBuilder(
            "arduino-cli", "core", "install", core,
            "--additional-urls", additionalUrl);
      } else {
        installCmd = new ProcessBuilder(
            "arduino-cli", "core", "install", core);
      }

      wsHandler.sendLog(sessionId, "Đang tải và cài đặt " + core + "...", "INFO");
      executeCommandWithProgress(installCmd, sessionId);

      wsHandler.sendLog(sessionId, "", "INFO");
      wsHandler.sendLog(sessionId, "Đã cài xong board " + core, "INFO");
      wsHandler.sendLog(sessionId, "", "INFO");

    } catch (Exception e) {
      wsHandler.sendLog(sessionId, "Lỗi khi cài board: " + e.getMessage(), "ERROR");
      throw new RuntimeException("Failed to install board", e);
    }
  }

  /**
   * On-demand: Đảm bảo libraries đã được cài đặt
   * Parse #include từ code và tự động cài libraries chưa có
   */
  private void ensureLibrariesInstalled(String sessionId, File arduinoFile) {
    try {
      // Đọc code từ file
      String code = Files.readString(arduinoFile.toPath());

      // Extract #include statements
      java.util.List<String> includes = extractIncludes(code);

      if (includes.isEmpty()) {
        return; // Không có library nào cần cài
      }

      // Check và install từng library
      for (String include : includes) {
        String libraryName = mapIncludeToLibrary(include);

        if (libraryName == null) {
          continue; // Built-in library, skip
        }

        // Check nếu library đã có
        if (isLibraryInstalled(libraryName)) {
          continue; // Đã có, skip
        }

        // Library chưa có → cài mới
        wsHandler.sendLog(sessionId, "", "INFO");
        wsHandler.sendLog(sessionId, "Library " + libraryName + " chưa có, đang cài...", "WARN");

        ProcessBuilder installCmd = new ProcessBuilder(
            "arduino-cli", "lib", "install", libraryName);

        executeCommandWithProgress(installCmd, sessionId);
        wsHandler.sendLog(sessionId, "Đã cài library " + libraryName, "INFO");
      }

    } catch (Exception e) {
      // Không throw error, chỉ warning vì có thể compile được mà không cần library
      wsHandler.sendLog(sessionId, "Không thể kiểm tra libraries: " + e.getMessage(), "WARN");
    }
  }

  /**
   * Extract #include statements từ code
   */
  private java.util.List<String> extractIncludes(String code) {
    java.util.List<String> includes = new java.util.ArrayList<>();

    // Pattern: #include <LibraryName.h> hoặc #include "LibraryName.h"
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "#include\\s*[<\"]([^>\"]+)[>\"]");
    java.util.regex.Matcher matcher = pattern.matcher(code);

    while (matcher.find()) {
      String include = matcher.group(1); // Ví dụ: "Adafruit_GFX.h"
      includes.add(include);
    }

    return includes;
  }

  /**
   * Map #include → library name trong Arduino Library Manager
   * Return null nếu là built-in library
   */
  private String mapIncludeToLibrary(String include) {
    // Remove .h extension
    String baseName = include.replace(".h", "");

    // Built-in libraries (không cần cài)
    java.util.Set<String> builtInLibs = java.util.Set.of(
        "Arduino", "EEPROM", "SPI", "Wire", "SoftwareSerial",
        "Servo", "SD", "Ethernet", "WiFi", "WiFiClient", "WiFiServer");

    if (builtInLibs.contains(baseName)) {
      return null; // Built-in, skip
    }

    // Known library mappings (header → library name)
    return switch (baseName) {
      // Adafruit libraries
      case "Adafruit_GFX" -> "Adafruit GFX Library";
      case "Adafruit_SSD1306" -> "Adafruit SSD1306";
      case "Adafruit_Sensor" -> "Adafruit Unified Sensor";
      case "DHT" -> "DHT sensor library";

      // ESP32/ESP8266 specific
      case "WiFiClientSecure" -> null; // Built-in cho ESP
      case "HTTPClient" -> null; // Built-in cho ESP
      case "WebServer" -> null; // Built-in cho ESP
      case "ESPAsyncWebServer" -> "ESP Async WebServer";

      // Popular libraries
      case "ArduinoJson" -> "ArduinoJson";
      case "PubSubClient" -> "PubSubClient";
      case "TFT_eSPI" -> "TFT_eSPI";
      case "LiquidCrystal_I2C" -> "LiquidCrystal I2C";
      case "OneWire" -> "OneWire";
      case "DallasTemperature" -> "DallasTemperature";

      // Default: Giữ nguyên tên
      default -> baseName;
    };
  }

  /**
   * Check xem library đã được cài chưa
   */
  private boolean isLibraryInstalled(String libraryName) {
    try {
      ProcessBuilder pb = new ProcessBuilder("arduino-cli", "lib", "list");
      String output = executeCommand(pb);
      return output.contains(libraryName);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Check xem core đã được cài chưa
   */
  private boolean isCoreInstalled(String core) {
    try {
      ProcessBuilder pb = new ProcessBuilder("arduino-cli", "core", "list");
      String output = executeCommand(pb);
      return output.contains(core);
    } catch (Exception e) {
      System.err.println("Error checking core installation: " + e.getMessage());
      return false;
    }
  }

  /**
   * Map core name → additional package URL
   */
  private String getBoardPackageUrl(String core) {
    return switch (core) {
      case "esp32:esp32" ->
        "https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json";
      case "esp8266:esp8266" -> "http://arduino.esp8266.com/stable/package_esp8266com_index.json";
      case "STMicroelectronics:stm32" ->
        "https://github.com/stm32duino/BoardManagerFiles/raw/main/package_stmicroelectronics_index.json";
      default -> null; // Arduino AVR không cần additional URL
    };
  }

  /**
   * Execute command và stream output với progress
   */
  private void executeCommandWithProgress(ProcessBuilder pb, String sessionId) throws Exception {
    pb.redirectErrorStream(true);
    Process process = pb.start();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {

      String line;
      while ((line = reader.readLine()) != null) {
        // Chỉ log dòng quan trọng
        if (line.contains("Downloading") || line.contains("Installing") ||
            line.contains("Downloaded") || line.contains("Installed")) {
          wsHandler.sendLog(sessionId, line, "INFO");
        }
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Command failed with exit code: " + exitCode);
    }
  }

  /**
   * Execute command và return output
   */
  private String executeCommand(ProcessBuilder pb) throws Exception {
    pb.redirectErrorStream(true);
    Process process = pb.start();

    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {

      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }

    process.waitFor();
    return output.toString();
  }

  // /**
  //  * Helper: Cleanup old build files trước khi compile
  //  * Xóa toàn bộ build folder của session
  //  */
  // private void cleanupOldBuildFiles(Path buildPath, String sessionId) {
  //   try {
  //     Files.walk(buildPath)
  //         .sorted((a, b) -> b.compareTo(a)) // Xóa file trước, folder sau
  //         .forEach(path -> {
  //           try {
  //             Files.delete(path);
  //             System.out.println("Deleted old build: " + path);
  //           } catch (IOException e) {
  //             System.err.println("Failed to delete: " + path);
  //           }
  //         });
  //     System.out.println("Cleaned up old build files for session: " + sessionId);
  //   } catch (IOException e) {
  //     System.err.println("Error cleaning old build files: " + e.getMessage());
  //   }
  // }
}
