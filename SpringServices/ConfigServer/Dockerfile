# Stage 1: Build với Maven
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# Build ứng dụng và giải nén layers từ file JAR
RUN ./mvnw clean package -DskipTests && \
    mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 2: Tạo image cuối
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy từng layer từ stage builder (theo thứ tự tối ưu của Spring Boot)
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Tối ưu JVM cho container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8888

# Chạy ứng dụng với Spring Boot Loader
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher"]