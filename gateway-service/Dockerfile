# 业务服务通用运行时 Dockerfile 模板说明见各服务 Dockerfile
# 本文件：gateway
FROM maven:3.9.9-eclipse-temurin-17
LABEL service="gateway-service"
WORKDIR /app
COPY gateway-service/target/gateway-service-3.6.0.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev,docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
