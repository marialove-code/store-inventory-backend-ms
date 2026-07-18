#!/bin/sh
# 一键停止微服务 jar（不停止 Nacos）

MS_HOME="${MS_HOME:-/www/wwwroot/inventory-ms-backend}"

stop_one() {
  name="$1"
  pids=$(pgrep -f "${name}-3.2.0.jar" 2>/dev/null || true)
  if [ -z "$pids" ]; then
    echo "未运行：$name"
    return 0
  fi
  echo "停止 $name : $pids"
  kill $pids 2>/dev/null || true
  sleep 2
  # 仍在则强杀
  pids=$(pgrep -f "${name}-3.2.0.jar" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    kill -9 $pids 2>/dev/null || true
    echo "  已强制停止 $name"
  fi
}

echo "==== 停止微服务 ===="
stop_one gateway-service
stop_one ai-service
stop_one order-service
stop_one inventory-service
stop_one platform-service
echo "完成（Nacos 未动，如需停止请到 nacos/bin 执行 shutdown.sh）"
