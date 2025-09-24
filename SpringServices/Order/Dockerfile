FROM eclipse-temurin:21-jre-jammy AS builder

WORKDIR /workspace

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

RUN java -Djarmode=layertools -jar app.jar extract

# Runtime distroless image
FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

# Copy các layer đã extract từ builder
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

# GC options
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:InitialRAMPercentage=50 \
-XX:MinRAMPercentage=50 \
-XX:MaxRAMPercentage=75 \
-XX:+UseG1GC \
-Xlog:gc*,gc+heap=debug:file=/logs/gc.log:time,uptime,level,tags"

EXPOSE 5050

# Distroless không có sh, nên phải chạy trực tiếp java
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-cp", "/app", "org.springframework.boot.loader.launch.JarLauncher"]
