# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy

# Tối ưu GC + RAM cho container
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xms256m -Xmx512m"

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 2005
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
