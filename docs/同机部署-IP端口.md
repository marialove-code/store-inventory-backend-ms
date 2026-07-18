# 同机部署微服务（IP:端口，不绑域名）

> 适用：腾讯云轻量 **2核4G + 宝塔**，上面已跑练习单体（域名 Nginx）。  
> 约定：微服务用 **公网 IP + 端口** 访问；**不改动**门店/单体现有域名站点。  
> Gateway 使用 **9080**（避开单体常见的 8080）。

服务器示例 IP：`120.53.88.204`（以你控制台为准）。

---

## 1. 端口与进程规划

| 进程 | 端口 | 是否对公网开放 |
|------|------|----------------|
| 练习单体 | 80/443 + 其 jar 端口 | 已有，**不动** |
| PostgreSQL / Redis | 5432 / 6379 | 仅本机，**不要**对公网乱开 |
| Nacos | 8848 | 建议仅本机；临时调试可开 |
| **gateway-service** | **9080** | **要开**（浏览器入口） |
| platform / inventory / order | 8081 / 8082 / 8083 | 仅本机（经 Gateway） |
| ai-service | 8084 | **第一期可先不起**（省内存） |
| Sentinel Dashboard | 8858 | **第一期可不起** |

4G 内存建议 JVM（每个业务进程）：

```text
-Xms128m -Xmx256m
```

Nacos 也可限制内存（见启动命令文档思路）。

---

## 2. 访问方式

```text
http://120.53.88.204:9080/api/order/ping
http://120.53.88.204:9080/api/...   ← 前端 API 根地址
```

本机前端联调：把 Vite 代理目标改成 `http://120.53.88.204:9080`（或环境变量），**不要**指到单体域名。

---

## 3. 你在宝塔 / 安全组要做的

### 3.1 腾讯云安全组

放行入站：**TCP 9080**（来源可先 `0.0.0.0/0` 练习用；以后再收紧）。

### 3.2 宝塔安全

若开了防火墙，同样放行 **9080**。

### 3.3 目录建议

```text
/www/wwwroot/ms/                 # 或 /opt/inventory-ms
  ├── jars/                      # 各服务 jar
  ├── nacos/                     # Nacos 解压目录
  ├── logs/
  ├── scripts/start-all.sh
  └── scripts/stop-all.sh
```

### 3.4 JDK

业务与 Nacos 用 **JDK 17**（与本地一致）。宝塔可装，或手动装 OpenJDK 17。

```bash
java -version   # 应看到 17
```

---

## 4. 数据库 / Redis

- 继续用本机已有 **PostgreSQL** 库（练习表，与门店那两张表无关）。  
- 启动时带环境变量（密码勿写进 git）：

```bash
export DB_PWD='你的数据库密码'
export REDIS_HOST=127.0.0.1
export REDIS_PWD='你的Redis密码'   # 无密码可空
```

`platform-service` 等已支持 `${DB_PWD}`、`${REDIS_HOST}`、`${REDIS_PWD}`。

---

## 5. Nacos

1. 上传 Nacos 2.x 到服务器，单机启动（JDK17 注意 `--add-opens`，见 [`本机启动命令-Nacos-Sentinel.md`](./本机启动命令-Nacos-Sentinel.md) 思路，Linux 用 `startup.sh -m standalone`）。  
2. 浏览器临时：`http://IP:8848/nacos`（若只内网，用 SSH 隧道）。  
3. 新建配置 DataId：`order-service.yaml`，Group：`DEFAULT_GROUP`，内容示例：

```yaml
app:
  nacos-demo-message: from-server-nacos
```

> order-service 启动会 `import` 该配置；没有的话可能启动失败。

---

## 5b. 配置说明（与单体相同拆法）

每个服务均为三份配置（对齐单体）：

| 文件 | 内容 |
|------|------|
| `application.yml` | 公共项 + `spring.profiles.active`（默认 `dev`，可用环境变量覆盖） |
| `application-dev.yml` | 本地：上传路径、跨域 localhost、Redis 等 |
| `application-prd.yml` | 服务器：`/opt/upload`、nestparts 跨域、Gateway 9080 等 |

- 本地：不用改，默认 `dev`  
- 服务器：`export SPRING_PROFILES_ACTIVE=prd`  
- 密码：`DB_PWD` / `REDIS_PWD` 走环境变量，勿写入仓库

## 6. 本机打包（你在 Windows 开发机执行）

在 `store-inventory-backend-ms` 根目录（有 Maven 时）：

```bash
mvn -pl gateway-service,platform-service,inventory-service,order-service -am clean package -DskipTests
```

取出 jar（路径以实际为准，一般为各模块 `target/*-*.jar`，排除 `.jar.original`）：

- `gateway-service/target/gateway-service-3.2.0.jar`（版本号以 pom 为准）  
- `platform-service/...`  
- `inventory-service/...`  
- `order-service/...`  

上传到服务器 `/www/wwwroot/ms/jars/`。

---

## 7. 启动顺序与命令（服务器上）

顺序：**Nacos → platform → inventory → order → gateway**。

示例（按实际 jar 名改）：

```bash
cd /www/wwwroot/ms
export DB_PWD='...'
export REDIS_HOST=127.0.0.1
export REDIS_PWD='...'

# platform
nohup java -Xms128m -Xmx256m -jar jars/platform-service-3.2.0.jar \
  > logs/platform.log 2>&1 &

# inventory
nohup java -Xms128m -Xmx256m -jar jars/inventory-service-3.2.0.jar \
  > logs/inventory.log 2>&1 &

# order（Sentinel Dashboard 可不起；规则仍可在进程内生效）
nohup java -Xms128m -Xmx256m \
  -Dcsp.sentinel.dashboard.server=127.0.0.1:8858 \
  -jar jars/order-service-3.2.0.jar \
  > logs/order.log 2>&1 &

# gateway 必须 9080，避开单体 8080
nohup java -Xms128m -Xmx256m -Dserver.port=9080 \
  -jar jars/gateway-service-3.2.0.jar \
  > logs/gateway.log 2>&1 &
```

查看：

```bash
ss -lntp | grep -E '9080|8081|8082|8083|8848'
curl -s http://127.0.0.1:9080/api/order/ping
```

公网：

```text
http://120.53.88.204:9080/api/order/ping
```

---

## 8. 和单体共存注意

1. **不要**改宝塔里已有域名的反向代理去指 Gateway。  
2. 演示微服务时，用 IP:9080；门店/练习单体继续走原域名。  
3. 两边若都写**同一套练习表**，别长时间双开狂点下单；门店那两张独立表不受影响。  
4. 内存报警：先停 ai、Sentinel 控制台；再减小 `-Xmx`；仍不够再考虑上班后新机。

---

## 9. 验收清单

- [ ] 安全组已放行 9080  
- [ ] Nacos 服务列表有 platform / inventory / order / gateway  
- [ ] `curl IP:9080/api/order/ping` 成功  
- [ ] 登录、下单走 Gateway 能通（前端指到 9080）  
- [ ] 原域名单体仍可打开  

---

## 10. 今日你先做的第一步（操作）

1. 腾讯云安全组放行 **9080**  
2. SSH/宝塔终端执行：`java -version`、`free -h`，把结果发我  
3. 确认本机 PostgreSQL 库名/账号（是否仍是 `inventory_store` / `inventory_user`）  

我根据你的回执，再带你装 Nacos 和上传 jar。
