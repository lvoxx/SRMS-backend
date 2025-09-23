# Runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy JAR với tên cụ thể
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Spring Boot layertools để extract các layer
RUN java -Djarmode=layertools -jar app.jar extract

# Copy logs folder để chứa GC logs (nếu muốn)
RUN mkdir -p /logs

ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:InitialRAMPercentage=50 \
-XX:MinRAMPercentage=50 \
-XX:MaxRAMPercentage=75 \
-XX:+UseG1GC \
-Xlog:gc*,gc+heap=debug:file=/logs/gc.log:time,uptime,level,tags"

EXPOSE 5050

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp app org.springframework.boot.loader.launch.JarLauncher"]
