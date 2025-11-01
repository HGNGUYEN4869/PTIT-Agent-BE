# Agent Chat - Hướng Dẫn Chạy Dự Án

## Yêu Cầu Hệ Thống

### Chạy với Docker (Khuyến nghị - Đơn giản nhất)

- **Docker**: Docker Desktop hoặc Docker Engine
- **Docker Compose**: Version 3.9+

### Chạy trực tiếp (Không dùng Docker)

- **Java**: JDK 21 hoặc cao hơn
- **Maven**: 3.6 hoặc cao hơn
- **MySQL**: 8.0 hoặc cao hơn
- **IDE**: IntelliJ IDEA, Eclipse, hoặc VS Code (khuyến nghị)

## 🚀 Cách 1: Chạy Với Docker (Khuyến Nghị)

### 1. Clone Repository

```bash
git clone <repository-url>
cd Chat-Agent/agent_chat
```

### 2. Build và Chạy với Docker Compose

```bash
# Build ứng dụng với Maven
mvn clean package -DskipTests

# Chạy Docker Compose (sẽ tự động tạo MySQL container và Spring Boot container)
docker-compose up -d
```

**Chú ý**: Lệnh này sẽ:

- Tạo MySQL container với database `appAgentDB`
- Tự động config connection string
- Chạy Spring Boot app trên port 8080
- MySQL chạy trên port 3307 (mapped từ 3306)

### 3. Kiểm Tra Ứng Dụng

```bash
# Xem logs
docker-compose logs -f app

# Kiểm tra containers đang chạy
docker ps

# Truy cập ứng dụng
http://localhost:8080
```

### 4. Dừng Ứng Dụng

```bash
# Dừng containers
docker-compose down

# Dừng và xóa volumes (xóa data)
docker-compose down -v
```

### Thông Tin Kết Nối MySQL (Docker)

```
Host: localhost
Port: 3307
Database: appAgentDB
Username: root
Password: 123456
```

## 🔧 Cách 2: Chạy Trực Tiếp (Không Dùng Docker)

### 1. Clone Repository

```bash
git clone <repository-url>
cd Chat-Agent/agent_chat
```

### 2. Cài Đặt và Cấu Hình MySQL

Tạo database mới trong MySQL:

```sql
CREATE DATABASE appAgentDB;
```

### 3. Cấu Hình Application Properties

Mở file `src/main/resources/application.properties` và cấu hình:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/appAgentDB?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server Port
server.port=8080
```

### 4. Cài Đặt Dependencies

```bash
mvn clean install
```

Hoặc skip tests:

```bash
mvn clean install -DskipTests
```

### 5. Chạy Ứng Dụng

#### Cách 1: Sử dụng Maven

```bash
mvn spring-boot:run
```

#### Cách 2: Sử dụng Maven Wrapper (nếu có)

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

#### Cách 3: Chạy file JAR

```bash
# Build JAR file
mvn clean package

# Chạy JAR
java -jar target/agent_chat-0.0.1-SNAPSHOT.jar
```

#### Cách 4: Từ IDE

- Mở project trong IntelliJ IDEA hoặc Eclipse
- Tìm class có annotation `@SpringBootApplication`
- Click chuột phải và chọn "Run"

### 6. Kiểm Tra Ứng Dụng

Sau khi chạy thành công, truy cập:

```
http://localhost:8080
```

Hoặc kiểm tra health check (nếu có):

```
http://localhost:8080/actuator/health
```

## API Documentation

Nếu dự án sử dụng Swagger/OpenAPI, truy cập:

```
http://localhost:8080/swagger-ui.html
```

## Cấu Trúc Thư Mục

```
agent_chat/
├── src/
│   ├── main/
│   │   ├── java/          # Source code Java
│   │   └── resources/     # Configuration files
│   └── test/              # Test files
├── target/                # Build output
├── docker-compose.yml     # Docker Compose configuration
├── Dockerfile            # Docker image configuration
├── pom.xml               # Maven configuration
└── README.md             # Documentation
```

## Docker Configuration Details

### docker-compose.yml

- **MySQL Service**:

  - Image: `mysql:8.0`
  - Port: `3307:3306` (host:container)
  - Database: `appAgentDB`
  - Root Password: `123456`
  - Volume: `db_data` (persistent storage)
  - Health check: Tự động kiểm tra MySQL sẵn sàng

- **Spring Boot App Service**:
  - Build từ Dockerfile
  - Port: `8080:8080`
  - Phụ thuộc vào MySQL (chờ MySQL healthy mới start)
  - Auto-restart on failure

### Dockerfile

- Base Image: `openjdk:21-jdk-slim`
- Sử dụng file JAR đã build: `agent_chat-0.0.1-SNAPSHOT.jar`
- Expose port: `8080`

## Troubleshooting

### Docker

#### Lỗi Port đã được sử dụng

```bash
# Dừng container đang chạy
docker-compose down

# Hoặc thay đổi port trong docker-compose.yml
ports:
  - "8081:8080"  # Thay 8080 thành port khác
```

#### MySQL container không healthy

```bash
# Xem logs MySQL
docker-compose logs db

# Restart containers
docker-compose restart

# Xóa volumes và tạo lại
docker-compose down -v
docker-compose up -d
```

#### Lỗi "Cannot connect to Docker daemon"

```bash
# Khởi động Docker Desktop hoặc Docker service
# Windows: Mở Docker Desktop
# Linux: sudo systemctl start docker
```

#### Rebuild lại containers

```bash
# Build lại JAR file
mvn clean package -DskipTests

# Rebuild và restart containers
docker-compose up -d --build
```

### Chạy Trực Tiếp (Không Docker)

#### Lỗi Port đã được sử dụng

```bash
# Thay đổi port trong application.properties
server.port=8081
```

#### Lỗi kết nối Database

- Kiểm tra MySQL service đã chạy chưa: `sudo systemctl status mysql`
- Kiểm tra database `appAgentDB` đã được tạo chưa
- Kiểm tra username/password đúng chưa
- Kiểm tra port MySQL (mặc định 3306)

#### Lỗi Maven dependencies

```bash
# Clear Maven cache và rebuild
mvn clean
mvn dependency:purge-local-repository
mvn clean install
```

#### Lỗi "Access denied for user 'root'@'localhost'"

```bash
# Reset MySQL password hoặc tạo user mới
mysql -u root -p
CREATE USER 'root'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON appAgentDB.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

## Môi Trường Development

### Hot Reload (Spring Boot DevTools)

Nếu đã thêm Spring Boot DevTools, ứng dụng sẽ tự động reload khi có thay đổi code.

```xml
<!-- Thêm vào pom.xml nếu chưa có -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

## Build cho Production

### Với Docker

```bash
# Build production image
docker build -t agent-chat:prod .

# Chạy production container
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://your-prod-db:3306/appAgentDB \
  -e SPRING_DATASOURCE_USERNAME=prod_user \
  -e SPRING_DATASOURCE_PASSWORD=prod_password \
  agent-chat:prod
```

### Không Docker

```bash
# Build với profile production
mvn clean package -Pprod

# Chạy với profile production
java -jar target/agent_chat-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Các Lệnh Docker Hữu Ích

```bash
# Xem logs real-time
docker-compose logs -f

# Xem logs của một service cụ thể
docker-compose logs -f app
docker-compose logs -f db

# Restart một service
docker-compose restart app

# Stop tất cả services
docker-compose stop

# Start lại services đã stop
docker-compose start

# Xem trạng thái containers
docker-compose ps

# Vào trong MySQL container
docker exec -it mysql_container mysql -uroot -p123456

# Vào shell của app container
docker exec -it spring_app bash

# Xóa tất cả (containers, networks, volumes)
docker-compose down -v
```

## Tài Liệu Tham Khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)

## Liên Hệ & Hỗ Trợ

Nếu gặp vấn đề, vui lòng tạo issue trên repository hoặc liên hệ team phát triển.
