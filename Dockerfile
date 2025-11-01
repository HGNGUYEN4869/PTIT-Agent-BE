# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy

# Cài Arduino CLI + Python3 (cần cho ESP32 toolchain)
RUN apt-get update && \
    apt-get install -y curl python3 && \
    curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh && \
    mv bin/arduino-cli /usr/local/bin/ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Cài Arduino AVR core (cho UNO, Nano, Mega...)
RUN arduino-cli core update-index && \
    arduino-cli core install arduino:avr

# TODO: Cài ESP32 sau nếu cần (file lớn, hay bị timeout)
# RUN arduino-cli core install esp32:esp32 --additional-urls https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json

# Tối ưu GC + RAM cho container
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xms256m -Xmx512m"

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 2005
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
