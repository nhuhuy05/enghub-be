# ========================================================
# Stage 1: Build stage (compile & package Spring Boot JAR)
# ========================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache layer cho dependencies (tối ưu tốc độ build)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy mã nguồn và đóng gói JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ========================================================
# Stage 2: Runtime stage (chạy app với Alpine JRE 21 siêu nhẹ)
# ========================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo non-root user để tăng tính bảo mật cho container
RUN addgroup -S enghub && adduser -S enghub -G enghub

# Copy file jar từ build stage và phân quyền cho non-root user
COPY --from=builder --chown=enghub:enghub /app/target/enghub-0.0.1-SNAPSHOT.jar app.jar

USER enghub

# Render tự động inject biến PORT (thường là 10000). 
# Spring Boot đọc server.port=${PORT:8080} nên sẽ tự khớp với port của Render.
EXPOSE 8080

# Cấu hình tối ưu bộ nhớ bắt buộc cho Render Free Tier (512MB RAM):
# - UseSerialGC: tiết kiệm 40MB RAM so với G1GC
# - Xmx220m: khống chế Heap tối đa 220MB
# - MaxMetaspaceSize: khống chế Metaspace 110MB
# - Xss256k: giảm 75% RAM của Thread stacks (mặc định 1MB/thread)
# Tổng dung lượng JVM tối đa ~385MB, luôn nằm an toàn dưới trần 512MB của Render
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -Xms128m -Xmx220m -XX:MaxMetaspaceSize=110m -Xss256k -XX:ReservedCodeCacheSize=40m -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
