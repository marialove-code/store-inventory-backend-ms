# =============================================================================
# order 运行时镜像（CI/CD 用 · 精简上下文）
# =============================================================================
# 目录里只需：本 Dockerfile + order-service-3.6.0.jar
# 由 scripts/deploy-order-docker.sh 在服务器上 docker build，不必拉整仓。
# =============================================================================

FROM maven:3.9.9-eclipse-temurin-17

LABEL service="order-service" \
      purpose="cicd-runtime"

WORKDIR /app

COPY order-service-3.6.0.jar /app/app.jar

EXPOSE 8083

# 服务器部署时用 docker run -e 覆盖为 prd
ENV SPRING_PROFILES_ACTIVE=prd \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
