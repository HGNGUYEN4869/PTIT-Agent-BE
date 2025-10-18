# Dựa trên JDK chính thức
FROM openjdk:21-jdk-slim

# Thư mục làm việc bên trong container
WORKDIR /app

# Copy file jar vào container
COPY target/agent_chat-0.0.1-SNAPSHOT.jar app.jar

# Cổng mà Spring Boot chạy
EXPOSE 8080

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
