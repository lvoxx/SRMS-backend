# Stage 1: Build with Maven (optimized caching)
FROM eclipse-temurin:21-jdk-jammy as builder

ARG SERVICE_NAME
ARG ARTIFACT_NAME

WORKDIR /app

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
FROM eclipse-temurin:21-jre-jammy

ARG SERVICE_NAME
ARG EXPOSED_PORT

WORKDIR /app

COPY --from=builder /app/${SERVICE_NAME}/target/extracted/dependencies/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/${SERVICE_NAME}/target/extracted/application/ ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
EXPOSE ${EXPOSED_PORT}
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher"]