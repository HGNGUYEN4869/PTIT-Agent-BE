# Agent Chat - Arduino IDE Backend

> Backend service for AI chat system and Arduino code compilation with real-time WebSocket logging

## Tech Stack

- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Database**: MongoDB 7.0
- **Build Tool**: Maven 3.9+
- **Security**: JWT (HttpOnly Cookies)
- **Real-time**: WebSocket
- **Arduino**: Arduino CLI (on-demand board/library installation)
- **Deployment**: Docker + Docker Compose

---

## Quick Start

### Option 1: Docker (Recommended)

```bash
# Build JAR
mvn clean package -DskipTests

# Start services
docker-compose up -d

# Check status
docker ps
curl http://localhost:2005/h/arduino/health
```

### Option 2: Local Development

```bash
# Install MongoDB, Arduino CLI first
# Configure application.properties

# Run
mvn spring-boot:run
```

---

## Architecture

```
Frontend (Next.js + React)
        ↓
Spring Boot Backend (Port 2005)
├── Controllers
│   ├── UserController     → /agent/auth/*
│   ├── ChatController     → /h/chats/*
│   └── ArduinoController  → /h/arduino/*
├── Services
│   └── ArduinoCompilerService (on-demand install)
├── WebSocket
│   └── CompileWebSocketHandler
└── Database (MongoDB)
    ├── users
    ├── chats
    └── messages
```

---

## API Endpoints

### 1. Authentication (`/agent/auth`)

**POST `/agent/auth/register`**
```json
{
  "userName": "john",
  "email": "john@example.com",
  "password": "password123"
}
```

**POST `/agent/auth/login`**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```
→ Returns HttpOnly cookies: `accessToken` (15min), `refreshToken` (7 days)

**GET `/agent/auth/me`**
- Auto-refresh token if expired
- Returns user info

**POST `/agent/auth/logout`**
```json
{
  "email": "john@example.com"
}
```

### 2. Chat System (`/h/chats`)

Requires `accessToken` cookie. See `ChatController.java` for full API.

### 3. Arduino Compiler (`/h/arduino`)

**WebSocket: `ws://localhost:2005/ws/compile/{sessionId}`**

Receives real-time logs:
```json
{
  "message": "Compilation successful!",
  "timestamp": "2025-11-01T10:30:00",
  "level": "SUCCESS"
}
```

**POST `/h/arduino/compile`**

Form-data:
- `file`: .ino file
- `sessionId`: WebSocket session ID
- `board`: Board FQBN (optional, default: `arduino:avr:uno`)

Supported boards:
- `arduino:avr:uno`, `arduino:avr:nano`, `arduino:avr:mega`
- `esp8266:esp8266:generic`
- `esp32:esp32:esp32`, `esp32:esp32:esp32s2`, `esp32:esp32:esp32s3`, `esp32:esp32:esp32c3`
- `STM32:stm32:GenF1`

**GET `/h/arduino/firmware/uno?sessionId={id}`**

Download compiled .hex firmware (auto-deletes after download)

**GET `/h/arduino/firmware/esp32?sessionId={id}`**

Download compiled .bin firmware

**GET `/h/arduino/firmware/stm32?sessionId={id}`**

Download compiled .dfu/.bin firmware

---

## Features

### On-Demand Board Installation

Automatically installs board cores when needed:

```
Board esp32:esp32 chưa có, đang cài...
Quá trình này có thể mất 2-5 phút

Đang cập nhật board index...
Đang tải và cài đặt esp32:esp32...
✅ Đã cài xong board esp32:esp32
```

Board package URLs:
- ESP32: `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
- ESP8266: `http://arduino.esp8266.com/stable/package_esp8266com_index.json`
- STM32: `https://github.com/stm32duino/BoardManagerFiles/raw/main/package_stmicroelectronics_index.json`

### On-Demand Library Installation

Parses `#include` statements and auto-installs missing libraries:

```
��� Library Adafruit GFX Library chưa có, đang cài...
✅ Đã cài library Adafruit GFX Library
```

**Supported libraries:**

| Header | Library Name |
|--------|--------------|
| `Adafruit_GFX.h` | Adafruit GFX Library |
| `Adafruit_SSD1306.h` | Adafruit SSD1306 |
| `DHT.h` | DHT sensor library |
| `ArduinoJson.h` | ArduinoJson |
| `PubSubClient.h` | PubSubClient |
| `TFT_eSPI.h` | TFT_eSPI |
| `LiquidCrystal_I2C.h` | LiquidCrystal I2C |
| `OneWire.h` | OneWire |
| `DallasTemperature.h` | DallasTemperature |

**Built-in (auto-skipped):** WiFi, SPI, Wire, EEPROM, Servo, SD, Ethernet

### Automatic Cache Cleanup

After firmware download:
1. Deletes `build/{sessionId}/` folder
2. Searches and removes Arduino cache files:
   - `~/.cache/arduino/sketches/`
   - `~/.arduino15/sketches/`
3. Removes empty directories
4. Clears session tracking

```
��� Cleaning up Arduino cache for session: abc-123
���️ Deleted: Blink.ino.hex
���️ Deleted: Blink.ino.elf
✅ Cleanup completed - deleted 5 items
```

### Docker Volume Persistence

Installed boards/libraries persist across container restarts:
```yaml
volumes:
  - arduino-data:/root/.arduino15
```

---

## Configuration

### application.properties

```properties
spring.application.name=agent_chat
spring.data.mongodb.uri=mongodb://root:123456@localhost:27017/appAgentDB?authSource=admin
spring.data.mongodb.database=appAgentDB
server.port=2005

# Logging
logging.level.root=WARN
logging.level.com.agent_chat=DEBUG
```

### docker-compose.yml

```yaml
services:
  db:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - db_data:/data/db
    
  app:
    build: .
    ports:
      - "2005:2005"
    volumes:
      - arduino-data:/root/.arduino15
    environment:
      JAVA_OPTS: -Xms256m -Xmx512m -XX:+UseG1GC
    deploy:
      resources:
        limits:
          cpus: "1"
          memory: 1G
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-jammy

# Install Arduino CLI
RUN curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh && \
    mv bin/arduino-cli /usr/local/bin/

# Install Arduino AVR core
RUN arduino-cli core update-index && \
    arduino-cli core install arduino:avr

# Copy JAR
COPY target/*.jar app.jar

EXPOSE 2005
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## Database Schema

### MongoDB Collections

**users**
```javascript
{
  _id: ObjectId,
  idUser: String (UUID),
  userName: String (unique),
  email: String (unique),
  password: String (hashed),
  accessToken: String,
  refreshToken: String
}
```

**chats**
```javascript
{
  _id: ObjectId,
  idChat: String (UUID),
  idUser: String (UUID),
  title: String,
  createdAt: ISODate,
  updatedAt: ISODate
}
```

**messages**
```javascript
{
  _id: ObjectId,
  idMessage: String (UUID),
  role: String ("USER" | "ASSISTANT"),
  content: String,
  idChat: String (UUID),
  createdAt: ISODate
}
```

---

## Development

### Install Arduino CLI

**Windows:**
```powershell
curl -fsSL https://github.com/arduino/arduino-cli/releases/download/v1.1.2/arduino-cli_1.1.2_Windows_64bit.zip -o arduino-cli.zip
Expand-Archive arduino-cli.zip -DestinationPath "C:\Program Files\ArduinoCLI"
$env:Path += ";C:\Program Files\ArduinoCLI"
```

**macOS/Linux:**
```bash
# macOS
brew install arduino-cli

# Linux
curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh
```

**Initialize:**
```bash
arduino-cli config init
arduino-cli core update-index
arduino-cli core install arduino:avr
```

### Run Tests

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Test compilation
curl -X POST http://localhost:2005/h/arduino/compile \
  -H "Cookie: accessToken=..." \
  -F "file=@sketch.ino" \
  -F "sessionId=test-123" \
  -F "board=arduino:avr:uno"
```

---

## Deployment

### Docker Commands

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f app

# Restart
docker-compose restart app

# Stop and remove
docker-compose down

# Rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Resource Monitoring

```bash
# Container stats
docker stats spring_app

# Arduino volume size
docker system df -v | grep arduino-data

# Check Arduino CLI
docker exec spring_app arduino-cli version
docker exec spring_app arduino-cli core list
```

---

## Troubleshooting

### arduino-cli not found

```bash
# Check PATH
echo $PATH

# Manual install
docker exec spring_app which arduino-cli
```

### Board not installed

```bash
# Manual install
docker exec spring_app arduino-cli core install esp32:esp32 \
  --additional-urls https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
```

### MongoDB connection failed

```bash
# Check MongoDB
docker logs mongodb_container

# Check connection string
docker exec spring_app env | grep MONGODB
```

### Out of memory

```yaml
# Increase Docker limits in docker-compose.yml
deploy:
  resources:
    limits:
      memory: 2G
```

---

## Security

- **JWT Tokens**: Stored in HttpOnly cookies (XSS-safe)
- **Password**: Hashed before storage
- **CORS**: Configured for specific origins
- **File Upload**: Validated (.ino only, 10MB max)
- **MongoDB**: Authentication enabled

**Production checklist:**
- [ ] Set `secure: true` for cookies (HTTPS)
- [ ] Use environment variables for secrets
- [ ] Enable SSL/TLS
- [ ] Configure firewall
- [ ] Regular security updates

---

## Project Structure

```
agent_chat/
├── src/
│   ├── main/
│   │   ├── java/com/agent_chat/
│   │   │   ├── Controller/
│   │   │   │   ├── ArduinoController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   └── UserController.java
│   │   │   ├── Service/
│   │   │   │   └── ArduinoCompilerService.java
│   │   │   ├── WebSocket/
│   │   │   │   └── CompileWebSocketHandler.java
│   │   │   ├── Entity/
│   │   │   ├── Repository/
│   │   │   ├── Config/
│   │   │   └── DTO/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── build/                  # Temporary firmware files
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Arduino CLI Documentation](https://arduino.github.io/arduino-cli/)
- [MongoDB Manual](https://www.mongodb.com/docs/manual/)
- [Docker Documentation](https://docs.docker.com/)

---

**Version**: 2.0.0  
**Last Updated**: November 1, 2025  
**Port**: 2005  
**Author**: Development Team
