# Stage 1: Build with Maven (optimized caching)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

ARG SERVICE_NAME
ARG EXPOSED_PORT
ARG ARTIFACT_NAME

# 1. Copy only POM files first for dependency caching
COPY pom.xml .
COPY ${SERVICE_NAME}/pom.xml ./${SERVICE_NAME}/

# 2. Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -B

# 3. Copy source code
COPY ${SERVICE_NAME}/src ./${SERVICE_NAME}/src

# 4. Build and extract layers
RUN mvn package -pl ${SERVICE_NAME} -am -DskipTests && \
    mkdir -p /app/${SERVICE_NAME}/target/extracted && \
    java -Djarmode=layertools -jar /app/${SERVICE_NAME}/target/${ARTIFACT_NAME}.jar extract \
    --destination /app/${SERVICE_NAME}/target/extracted

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG SERVICE_NAME
ARG EXPOSED_PORT
ARG ARTIFACT_NAME

COPY --from=builder /app/${SERVICE_NAME}/target/extracted/dependencies/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/application/ ./

ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:InitialRAMPercentage=50 \
-XX:MinRAMPercentage=50 \
-XX:MaxRAMPercentage=75 \
-XX:+UseG1GC \
-XX:ActiveProcessorCount=2 \
-Xlog:gc*,gc+heap=debug:file=/logs/gc.log:time,uptime,level,tags"

EXPOSE ${EXPOSED_PORT}
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]