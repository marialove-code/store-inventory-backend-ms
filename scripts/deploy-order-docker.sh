#!/bin/sh
# =============================================================================
# 部署 order 容器（中厂实用 CD：换 jar → 重建镜像 → 重启 → 探活）
# =============================================================================
# 用法：
#   bash deploy-order-docker.sh [jar路径]
# 默认 jar：
#   /www/wwwroot/inventory-ms-backend/deploy/order-runtime/order-service-3.6.0.jar
#
# 回滚示例（上一版 jar 会留在 *.prev）：
#   bash deploy-order-docker.sh .../order-service-3.6.0.jar.prev
# =============================================================================

set -e

MS_HOME="${MS_HOME:-/www/wwwroot/inventory-ms-backend}"
RUNTIME_DIR="${RUNTIME_DIR:-$MS_HOME/deploy/order-runtime}"
ENV_FILE="${ENV_FILE:-/opt/inventory-ms/env.sh}"
IMAGE_TAG="${IMAGE_TAG:-inventory-order:3.6.0}"
CONTAINER_NAME="${CONTAINER_NAME:-order-c}"
JAR_NAME="order-service-3.6.0.jar"
DEST_JAR="$RUNTIME_DIR/$JAR_NAME"
PING_URL="${PING_URL:-http://127.0.0.1:8083/api/order/ping}"
PING_RETRIES="${PING_RETRIES:-30}"
PING_INTERVAL_SEC="${PING_INTERVAL_SEC:-3}"

SRC_JAR="${1:-$DEST_JAR}"

echo "==== order Docker 部署开始 ===="
echo "  jar=$SRC_JAR"
echo "  runtime=$RUNTIME_DIR"
echo "  image=$IMAGE_TAG"
echo "  container=$CONTAINER_NAME"

if [ ! -f "$SRC_JAR" ]; then
  echo "错误：找不到 jar：$SRC_JAR"
  exit 1
fi

if [ ! -f "$RUNTIME_DIR/Dockerfile" ]; then
  echo "错误：找不到 Dockerfile：$RUNTIME_DIR/Dockerfile"
  echo "请先把仓库里的 deploy/order-runtime/Dockerfile 放到该目录"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "错误：找不到环境文件：$ENV_FILE"
  exit 1
fi

mkdir -p "$RUNTIME_DIR"

# 覆盖前备份当前 jar，便于回滚
if [ -f "$DEST_JAR" ]; then
  # 若源就是 dest（原地部署），不要把自己拷成 prev 再踩掉
  src_real=$(cd "$(dirname "$SRC_JAR")" && pwd)/$(basename "$SRC_JAR")
  dest_real=$(cd "$RUNTIME_DIR" && pwd)/$JAR_NAME
  if [ "$src_real" != "$dest_real" ]; then
    echo "备份旧 jar → ${DEST_JAR}.prev"
    cp -f "$DEST_JAR" "${DEST_JAR}.prev"
    echo "拷贝新 jar → $DEST_JAR"
    cp -f "$SRC_JAR" "$DEST_JAR"
  fi
else
  echo "拷贝 jar → $DEST_JAR"
  cp -f "$SRC_JAR" "$DEST_JAR"
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

echo "构建镜像 $IMAGE_TAG ..."
docker build -t "$IMAGE_TAG" "$RUNTIME_DIR"

echo "停止旧容器（若有）..."
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

echo "启动容器 $CONTAINER_NAME ..."
docker run -d --name "$CONTAINER_NAME" --network host \
  -e SPRING_PROFILES_ACTIVE=prd \
  -e DB_PWD \
  -e REDIS_HOST \
  -e REDIS_PWD \
  -e DASHSCOPE_API_KEY \
  "$IMAGE_TAG"

echo "等待探活 $PING_URL ..."
i=0
ok=0
while [ "$i" -lt "$PING_RETRIES" ]; do
  i=$((i + 1))
  body=$(curl -s -m 3 "$PING_URL" 2>/dev/null || true)
  case "$body" in
    *'"code":200'*)
      ok=1
      echo "探活成功（第 ${i} 次）：$body"
      break
      ;;
  esac
  echo "  第 ${i}/${PING_RETRIES} 次未就绪，${PING_INTERVAL_SEC}s 后重试..."
  sleep "$PING_INTERVAL_SEC"
done

if [ "$ok" -ne 1 ]; then
  echo "错误：探活失败，最近日志："
  docker logs --tail 80 "$CONTAINER_NAME" || true
  exit 1
fi

echo "==== order Docker 部署成功 ===="
docker ps --filter "name=$CONTAINER_NAME"
