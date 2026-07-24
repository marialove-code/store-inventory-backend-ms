# =============================================================================
# order-service · Dockerfile（云原生 Day1 · 推荐先用这个）
# =============================================================================
#
# 【这个文件干什么】
#   把「已经在本机打好的 order-service.jar」打进镜像并规定如何启动。
#   镜像 ≈ 安装包；容器 ≈ 运行中的订单服务进程。
#
# 【为什么 Day1 先本机编译、镜像只负责运行】
#   1. 本机 Maven 往往已有依赖缓存，比容器里重新下依赖更稳；
#   2. 网络不稳时，容器内 mvn 容易失败（你刚可能遇到）；
#   3. 先搞懂「镜像里跑 jar」这一步；多阶段在容器里编译见 Dockerfile.multistage。
#
# 【使用步骤】
#   # ① 仓库根目录，本机先出包
#   mvn -pl order-service -am -DskipTests package
#
#   # ② 再构建镜像（上下文仍是仓库根，便于 COPY 到 jar 路径）
#   docker build -f order-service/Dockerfile -t inventory-order:3.6.0 .
#
# 【和 K8s 的关系】
#   本文件只造镜像。K8s 是以后用镜像起很多副本；Day1 用 docker run 即可。
# =============================================================================

# 使用本机已有的基础镜像（避免 Docker Hub 拉不下）
# 网络好时可换成更小的：eclipse-temurin:17-jre-jammy
FROM maven:3.9.9-eclipse-temurin-17

LABEL maintainer="inventory-ms-learning" \
      service="order-service" \
      description="进销存订单服务（学习用：本机 jar + 容器运行）"

WORKDIR /app

# 明确版本号，避免 target 里残留旧 jar 时通配拷错
COPY order-service/target/order-service-3.6.0.jar /app/app.jar

EXPOSE 8083

# dev：压测等；docker：中间件指向宿主机（application-docker.yml）
ENV SPRING_PROFILES_ACTIVE=dev,docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# 容器内存与堆：MaxRAMPercentage 按容器限额估堆（面试常考，别只记死 -Xmx）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
