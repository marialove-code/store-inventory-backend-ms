#!/bin/sh
# 一键启动微服务（服务器使用）
# 用法：
#   1) 先准备 /opt/inventory-ms/env.sh（见 env.sh.example）
#   2) chmod +x start-all.sh
#   3) ./start-all.sh
#
# 默认目录：/www/wwwroot/inventory-ms-backend
# 可通过环境变量 MS_HOME 覆盖

set -e

MS_HOME="${MS_HOME:-/www/wwwroot/inventory-ms-backend}"
ENV_FILE="${ENV_FILE:-/opt/inventory-ms/env.sh}"
JARS="$MS_HOME/jars"
LOGS="$MS_HOME/logs"

if [ ! -f "$ENV_FILE" ]; then
  echo "缺少环境文件：$ENV_FILE"
  echo "请先：mkdir -p /opt/inventory-ms && cp env.sh.example 到该路径并 chmod 600、填入密码"
  exit 1
fi

# shellcheck disable=SC1090
. "$ENV_FILE"

# env.sh 若写坏 PATH 或带 Windows 换行，会导致 mkdir/ss 找不到；这里强制补回系统路径
export PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${PATH}"

if [ ! -x "$JAVA_HOME/bin/java" ] && ! command -v java >/dev/null 2>&1; then
  echo "未找到 java，请检查 JAVA_HOME=$JAVA_HOME"
  exit 1
fi
JAVA="${JAVA_HOME}/bin/java"
[ -x "$JAVA" ] || JAVA="java"

/usr/bin/mkdir -p "$LOGS" /opt/upload/avatar /opt/upload/product /opt/upload/brand

# 检查 Nacos
if ! /usr/bin/ss -lntp 2>/dev/null | grep -q ':8848 '; then
  echo "警告：未检测到 8848 端口，请先启动 Nacos："
  echo "  cd $MS_HOME/nacos/bin && sh startup.sh -m standalone"
  exit 1
fi

start_one() {
  name="$1"
  jar="$JARS/${name}-3.2.0.jar"
  log="$LOGS/${name}.log"
  if [ ! -f "$jar" ]; then
    echo "跳过 $name：找不到 $jar"
    return 0
  fi
  # 已在跑则跳过
  if pgrep -f "${name}-3.2.0.jar" >/dev/null 2>&1; then
    echo "已在运行：$name"
    return 0
  fi
  echo "启动 $name ..."
  nohup "$JAVA" -Xms128m -Xmx256m -jar "$jar" >>"$log" 2>&1 &
  echo "  pid=$!  log=$log"
  sleep "$2"
}

echo "==== 启动微服务 profile=$SPRING_PROFILES_ACTIVE ===="
start_one platform-service 25
start_one inventory-service 20
start_one order-service 20
# AI 可选：内存紧可注释下一行
start_one ai-service 15
start_one gateway-service 10

echo "==== 端口检查 ===="
ss -lntp 2>/dev/null | grep -E '8848|8081|8082|8083|8084|9080' || true
echo "完成。Gateway 示例：curl -s http://127.0.0.1:9080/api/order/ping"
echo "日志目录：$LOGS"
