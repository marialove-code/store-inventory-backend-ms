# 云原生 Day1 · order-service 容器化（操作说明）

> 先读概念：[`云原生-Docker与K8s入门.md`](./云原生-Docker与K8s入门.md)  
> 本页只讲：**怎么把订单服务打成镜像并跑起来**。

---

## 1. 文件落点

| 文件 | 作用 |
|------|------|
| `order-service/Dockerfile` | 造镜像的说明书（多阶段：编译 → 运行） |
| `order-service/src/main/resources/application-docker.yml` | 容器专用配置：中间件改指宿主机 |
| 仓库根 `.dockerignore` | 减小 build 上下文（别把 target、.git 打进去） |

探活地址（context-path=`/api`）：

```text
http://localhost:8083/api/order/ping
```

---

## 2. 构建镜像（在仓库根目录执行）

Day1 **推荐两步**（本机编译稳，少受容器内下依赖影响）：

```powershell
cd E:\Projects\store-inventory-backend-ms

# ① 本机出包（用你本机 Maven 缓存；无 mvn 时可用 Docker 挂载 .m2，见下文）
mvn -pl order-service -am -DskipTests package
# 建议删掉 target 里旧版本 jar，只留 order-service-3.6.0.jar

# ② 把 jar 打进镜像（Dockerfile 只 COPY 指定版本 jar）
docker build -f order-service/Dockerfile -t inventory-order:3.6.0 .
```

若本机没有 `mvn` 命令，可用已有 Maven 镜像挂载工程与 `.m2`：

```powershell
docker run --rm `
  -v "E:/Projects/store-inventory-backend-ms:/ws" `
  -v "$env:USERPROFILE/.m2:/root/.m2" `
  -w /ws maven:3.9.9-eclipse-temurin-17 `
  mvn -pl order-service -am -DskipTests package
```

进阶（容器内多阶段编译，网络好时再试）：

```powershell
docker build -f order-service/Dockerfile.multistage -t inventory-order:3.6.0 .
```

成功后：

```powershell
docker images inventory-order
```

---

## 3. 运行容器（中间件仍在宿主机）

前置：本机 Nacos `8848`、PG `5432`、按需 Redis/RabbitMQ 已开；`DB_PWD` 等环境变量与平时一致。

```powershell
docker rm -f order-c 2>$null

# Redis：与平时本机跑 order 一致（公网或隧道）；不要用 127.0.0.1（容器里那是容器自己）
# 你平时 application-dev 默认是 120.53.88.204，可原样传入
docker run --name order-c -d -p 8083:8083 `
  --add-host=host.docker.internal:host-gateway `
  -e SPRING_PROFILES_ACTIVE=dev,docker `
  -e DB_PWD="$env:DB_PWD" `
  -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 `
  -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=host.docker.internal:8848 `
  -e SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=host.docker.internal:8848 `
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/inventory_store" `
  -e REDIS_HOST=120.53.88.204 `
  -e SPRING_RABBITMQ_HOST=host.docker.internal `
  inventory-order:3.6.0
```

说明：

- `-p 8083:8083`：宿主机 8083 → 容器 8083（先停掉本机已起的 order）
- `dev,docker`：压测开关 + 容器网络配置
- **Nacos / PG**：经 `host.docker.internal` 访问宿主机；需本机 8848、5432（隧道）已开
- **Redis**：用你平时能通的地址（示例公网）；容器访问 `127.0.0.1:6379` 会失败
- `DB_PWD`：与平时启动微服务相同（PowerShell 里先 `$env:DB_PWD="你的密码"`）

看日志：

```powershell
docker logs -f order-c
```

探活：

```powershell
curl http://localhost:8083/api/order/ping
```

停掉：

```powershell
docker stop order-c
docker rm order-c
```

---

## 4. 常见坑

| 现象 | 原因 | 处理 |
|------|------|------|
| 连不上 Nacos/DB | 容器里 `127.0.0.1` 是容器自己 | 确认 profile 含 `docker` |
| 端口占用 | 宿主机已有 java 占 8083 | 先停本机 order，或换 `-p 18083:8083` |
| Nacos 有实例但调不通 | 注册 IP 不对 | docker 配置里保持 `127.0.0.1` + 端口映射 |
| build 很慢 | 拉 Maven 镜像 / 下载依赖 | 等一次即可；可配置 Docker 镜像加速 |

---

## 5. Day1 验收勾选

- [ ] 能讲清 Docker / K8s / 二者区别（入门文）  
- [ ] `docker build` 成功得到 `inventory-order:3.6.0`  
- [ ] `docker run` 后 ping 返回 200  
- [ ] （可选）Nacos 控制台能看到 `order-service`  

下一步 Day2：给 gateway 同样套路 + `docker-compose.dev.yml`。
