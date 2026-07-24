# 第 10 批：云原生 Docker / Kubernetes 面试题

> 索引：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)  
> 互补：[云原生-总览与实操.md](./云原生-总览与实操.md) · [CI-CD-order中厂自动化部署.md](./CI-CD-order中厂自动化部署.md) · [部署运维-总览.md](./部署运维-总览.md)  
> 本批共 **8 题** · **全 10 批已完成**

---

## 题 1：Docker 核心概念与价值

================

## 面试问题：

Docker 是什么？镜像、容器、Dockerfile 分别是什么？为什么微服务项目要用容器？

## 考察点：

- 容器 vs 虚拟机
- 可复制交付单元
- 结合进销存 order 镜像

## 标准答案：

**Docker**：容器引擎，把应用与依赖打包成 **可移植的运行单元**。

| 概念 | 类比 | 说明 |
|------|------|------|
| **镜像 Image** | 安装包 / 只读模板 | `inventory-order:3.6.0` |
| **容器 Container** | 运行中的程序实例 | 服务器上的 `order-c` |
| **Dockerfile** | 造镜像的说明书 | 基础镜像 + COPY jar + ENTRYPOINT |
| **Registry** | 应用商店 | 本机 load/import，或私有仓库 |

**容器 vs 虚拟机**：

- VM：完整 OS，重、启动慢
- 容器：**共享宿主机内核**，隔离进程/文件系统/网络，轻、秒级启动

**为什么用**：

- **环境一致**：本机 build 的镜像在腾讯云跑一样
- **交付标准**：jar + Dockerfile，不靠「某台机器手工配 JDK」
- **为编排铺路**：Compose / K8s 管多容器

## 通俗理解：

镜像像 **密封餐盒**（菜和餐具都配好）；容器像 **加热开吃的那一盒**；Dockerfile 像 **菜谱**。换台机器只要 **同一餐盒**，不用重新学厨师习惯。

## 项目结合：

```powershell
# 本机构建
mvn -pl order-service -am -DskipTests package
docker build -f order-service/Dockerfile -t inventory-order:3.6.0 .
docker run ... inventory-order:3.6.0
# 探活：http://localhost:8083/api/order/ping
```

- 服务器：`docker load` 或 CD 传 jar → `deploy-order-docker.sh` 重建 `order-c`
- 中间件 **Nacos/PG/Redis/Nginx** 刻意留 **宿主机**，业务 jar 容器化 — **务实中厂**

## 面试官追问：

1. 容器数据会丢吗？
2. docker commit 和 Dockerfile build 区别？
3. 镜像分层缓存干什么用？

## 高级回答：

- **可写层** 容器删了丢；持久化用 **Volume** 或数据放宿主机 PG。
- **Dockerfile build** 可重复、可 CI；commit 不可维护。
- **分层**：改 jar 只重建最后几层，加速 build。
- 10 年答法：**「我 order 已 Docker 化；镜像=交付物，不是虚拟机替身」**。

================

---

## 题 2：Dockerfile 编写与多阶段构建

================

## 面试问题：

写一个 Java Spring Boot 服务的 Dockerfile 要注意什么？什么是多阶段构建？你们项目怎么做的？

## 考察点：

- 基础镜像选型（eclipse-temurin）
- 非 root、时区、JVM 参数
- 运行时镜像 vs 构建镜像分离

## 标准答案：

**常见写法（运行时镜像 + 外挂 jar）**：

```dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY order-service/target/order-service-3.6.0.jar app.jar
ENV TZ=Asia/Shanghai
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**注意点**：

| 项 | 建议 |
|----|------|
| 基础镜像 | **JRE** 而非 JDK（运行够用） |
| 时区 | `TZ=Asia/Shanghai` |
| 端口 | EXPOSE 文档化，`-p` 或 host 网络映射 |
| 密钥 | **不进镜像**，`env_file` / 挂载 `env.sh` |
| 用户 | 生产可 `USER nonroot`（加分） |

**多阶段构建**：

```dockerfile
# stage1: Maven 编译
FROM maven:3.9-eclipse-temurin-17 AS builder
COPY . .
RUN mvn -pl order-service -am -DskipTests package

# stage2: 只带 jar 的瘦镜像
FROM eclipse-temurin:17-jre-jammy
COPY --from=builder /app/order-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

**优点**：最终镜像 **不含 Maven 源码**，体积小；CI 可在 Docker 内完整 build。

## 通俗理解：

多阶段像 **厨房做好菜再装外卖盒**——顾客只收到盒子，不会收到整间厨房和生食材。

## 项目结合：

- 本机：`order-service/Dockerfile` + 外挂 jar（Maven 在宿主机打）
- 进阶：`order-service/Dockerfile.multistage`（CI 内一键 build）
- 服务器：`deploy/order-runtime/Dockerfile` + CD 替换 jar 文件名（Gitee 部署包 **无点号** 约束）

```bash
# deploy-order-docker.sh 核心逻辑（概念）
docker stop order-c || true
docker rm order-c || true
docker build -t inventory-order:3.6.0 .
docker run -d --name order-c --network host --env-file /opt/inventory-ms/env.sh inventory-order:3.6.0
curl http://127.0.0.1:8083/api/order/ping
```

## 面试官追问：

1. CMD 和 ENTRYPOINT 区别？
2. 为什么镜像里不建议 apt 装太多？
3. .dockerignore 有什么用？

## 高级回答：

- **ENTRYPOINT** 固定主进程；**CMD** 默认参数可被覆盖。
- 镜像越大 **拉取越慢、攻击面越大**；瘦 JRE 镜像即可。
- **.dockerignore** 排除 `target/`、`.git`，减小 context、加速 build。
- 10 年答法：**「我两种 Dockerfile 都备；生产 CD 换 jar 重建容器，版本 tag 对齐 3.6.0」**。

================

---

## 题 3：Docker 网络、Compose 与 host 模式

================

## 面试问题：

Docker 网络模式有哪些？为什么服务器 order 用 `--network host`？docker-compose 和 K8s 区别？

## 考察点：

- bridge / host / none
- 本机五服务 compose vs 生产单机 order
- compose ≠ K8s

## 标准答案：

**常见网络模式**：

| 模式 | 行为 | 场景 |
|------|------|------|
| **bridge** | 默认，容器独立 IP，端口映射 `-p` | 本机多容器 |
| **host** | 用 **宿主机网络栈**，无 NAT | 与宿主机 Nacos/PG 同机，省端口映射麻烦 |
| **none** | 无网卡 | 安全隔离作业 |

**进销存服务器 order-c 用 host**：

- Nacos、PostgreSQL、Redis 在 **127.0.0.1 宿主机**
- 容器内 `localhost:8848` **若用 bridge 指向容器自己**，连不上宿主机 Nacos
- host 模式：order **直接监听 8083**，与 jar 直跑行为一致

**docker-compose**：

- **单机多容器** 编排：起 gateway、order、inventory…
- 文件 `docker-compose.dev.yml`；本机五服务练习
- **不是 K8s**：无跨机调度、无 Pod 自愈、无 HPA

```text
Docker     → 一台机器上的「餐盒」
Compose    → 同一桌上一组餐盒一起上
Kubernetes → 多店厨房统一调度、扩份数、换坏盒
```

## 通俗理解：

bridge 像 **每个容器单独房间，门口开转接号**；host 像 **直接在客厅办公，和家里电话同一套线路**——和宿主机 Nacos 通话不用转接。

## 项目结合：

- 本机五服务：见 [云原生-五服务容器启动.md](./云原生-五服务容器启动.md)
- 中间件 **不进 compose**（Nacos/PG/Redis 宿主机；ES/Rabbit 可选 Desktop 容器）
- 生产：**仅 order 容器** + 其余可 jar — **范围控制**

## 面试官追问：

1. 容器如何访问宿主机服务（Windows/Mac）？
2. `-p 8083:8083` 和 host 二选一？
3. compose 的 depends_on 能保证依赖就绪吗？

## 高级回答：

- **host.docker.internal**（Desktop）；Linux 可用 **host 网关 IP** 或 host 网络。
- **depends_on** 只保证 **启动顺序**，不保证 Nacos **已 ready** — 要 **healthcheck + retry** 或 `spring.cloud.nacos.config.fail-fast=false` 重试。
- 10 年答法：**「服务器 host 网络是为同机 Nacos/PG；compose 本机练五服务；K8s 只口述」**。

================

---

## 题 4：CI/CD — Gitee Go 与手动 CD

================

## 面试问题：

你们 CI/CD 怎么做的？为什么 push 只 CI、CD 手动确认？和中厂无专职运维怎么匹配？

## 考察点：

- 流水线阶段、制品、主机组
- 安全（不自动上生产）
- 真实文件路径

## 标准答案：

**流程（进销存已跑通）**：

```text
开发者 push ms/dev
  → Order-CI（Gitee Go 云端 Maven 编译）~10min
  → 产出 order-service jar 制品

手动触发 Order-CD / 打 tag
  → 主机组 inventory-prd（Agent 在线）
  → 部署阶段「待手动执行」→ 点确认
  → deploy-order-docker.sh：换 jar、docker build、重启 order-c
  → curl /api/order/ping 探活
```

**为什么 CD 手动**：

- 家用轻量云 = **现网**，避免 push 误触全量上线
- **无专职运维**，发布要人眼看一眼
- 符合中厂：**CI 自动化编译，CD 可审批**

**关键文件**：

| 文件 | 作用 |
|------|------|
| `.workflow/order-ci.yml` | push 触发 Maven |
| `.workflow/order-cd.yml` | `hostGroupID: inventory-prd` |
| `scripts/deploy-order-docker.sh` | 服务器部署 |
| `deploy/order-runtime/Dockerfile` | 运行时镜像 |

**坑**：Gitee 部署文件名 **不能含多个点** → 制品名 `order-service-360-jar`，脚本内 rename 为 `order-service-3.6.0.jar`

## 通俗理解：

CI 像 **中央厨房统一做半成品**；CD 像 **店长签字才运到门店加热上架**——不是每做一批菜就自动塞给顾客。

## 项目结合：

- 仓库：`maria_love/store-inventory-backend-ms`，分支 `ms/dev`
- 主机：`VM-0-5-opencloudos`，Agent 在线
- Order-CD #1 已绿：编译 → 手动部署 → 探活成功
- 环境变量：`/opt/inventory-ms/env.sh`（DB、Nacos、JWT **不进 Git**）

## 面试官追问：

1. CI 和 CD 制品存在哪？
2. 回滚怎么做？
3. Jenkins 和 Gitee Go 怎么选？

## 高级回答：

- **回滚**：保留上一版 jar/镜像 tag，`docker run` 旧 tag；或 Git revert + 再 CD。
- **Jenkins**：自建灵活、运维成本高；**Gitee Go** 与码云一体，中小团队够用。
- 10 年答法：**「我能讲清从 push 到 ping 全链路；强调手动 CD 是刻意设计不是不会自动化」**。

================

---

## 题 5：容器资源、JVM 与 OOMKilled

================

## 面试问题：

容器 memory limit 和 JVM -Xmx 什么关系？为什么会 OOMKilled？怎么配 Java 容器参数？

## 考察点：

- 与 JVM 批衔接
- cgroup 限制
- 生产可讲 order 容器经验

## 标准答案：

**容器 limit 包含**：

```text
JVM Heap + Metaspace + 线程栈 + CodeCache + DirectMemory + Native
```

**`-Xmx` 接近 limit → 必 OOMKilled**（Linux killer 杀容器，不是 Java OOM）。

**推荐（JDK 11+）**：

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs"
```

**排查**：

```bash
docker inspect order-c --format '{{.State.OOMKilled}}'
dmesg | grep -i oom
```

## 通俗理解：

容器 limit 是 **整个办公室租金**；JVM 堆只是 **其中一个仓库**。仓库租满整间办公室，连走廊都没了，物业直接 **清场（OOMKilled）**。

## 项目结合：

- 轻量云 **1G 内存** 跑 order 容器：limit 1G 时 **不要 -Xmx1024m**
- `env.sh` 注入 `JAVA_OPTS`，CD 脚本 `--env-file`
- 与 [面试题-02-JVM.md](./面试题-02-JVM.md) 题 8 同一套答法

## 面试官追问：

1. CPU limit 影响 GC 吗？
2. 如何看容器实际内存？
3. jmap 能在容器里用吗？

## 高级回答：

- **CPU throttle** 会拉长 STW，P99 变差。
- `docker stats order-c`；Prometheus cAdvisor。
- **jmap** 需 JDK 工具镜像或 `docker exec` 进容器用 jcmd。
- 10 年答法：**「容器 Java 必 MaxRAMPercentage，留 25% 非堆；OOMKilled 先查 limit 不是 leak」**。

================

---

## 题 6：Kubernetes 核心概念（口述版）

================

## 面试问题：

Kubernetes 解决什么问题？Pod、Deployment、Service、Ingress 分别是什么？和 Docker 什么关系？

## 考察点：

- 项目 **未上 K8s**，但能口述
- 与微服务、Nacos、Gateway 挂钩
- 不夸大个人经验

## 标准答案：

**K8s 解决**：**多机** 上容器的 **调度、扩缩、自愈、滚动发布、服务发现**。

| 资源 | 作用 |
|------|------|
| **Pod** | 最小调度单元，通常 1 容器（或多容器 sidecar） |
| **Deployment** | 声明 **副本数、镜像版本**，滚动升级 |
| **Service** | 集群内 **稳定虚拟 IP/DNS**，负载到 Pod |
| **Ingress** | **七层路由**，类似集群版 Nginx + Gateway |
| **ConfigMap/Secret** | 配置与密钥 |
| **HPA** | 按 CPU/QPS 自动扩缩副本 |

**与 Docker**：K8s **调度的还是容器**（containerd/CRI）；Docker 是构建/单机运行，K8s 是 **编排层**。

**若进销存上 K8s（口述）**：

```text
Ingress(nestparts.top) → gateway Deployment(2 replicas)
  → Service order-service → order Pod × N
  → Nacos 仍可注册，或改用 K8s Service + 内部 DNS
```

## 通俗理解：

Docker 是 **餐盒**；K8s 是 **连锁总部的调度系统**——哪个店缺货自动 **再送 3 盒**、坏盒 **自动换**、新店 **滚动开业** 不影响营业。

## 项目结合：

**诚实边界**：

- ✅ 已做：Docker、Compose、order 容器、CI/CD
- 📖 口述：K8s Pod/Deployment/Service、滚动发布、探针
- ❌ 未做：真实 K8s 集群、Helm、Operator

**与并发演进挂钩**（云原生总览）：

- 多副本 order → **V5 Redis 锁** 仍有意义（跨 Pod）
- 滚动发布重复请求 → **V7 幂等**
- 探针失败重启 → 要 **优雅停机**（处理完请求再 exit）

## 面试官追问：

1. liveness 和 readiness 探针区别？
2. Pod 漂移 IP 怎么调？
3. StatefulSet 和 Deployment 区别？

## 高级回答：

- **readiness** 失败从 Service 摘掉；**liveness** 失败重启 Pod。
- 通过 **Service 名称** 调用，不绑 Pod IP。
- **StatefulSet**：稳定网络标识、有序部署（Kafka、ZK）；无状态 Java 用 Deployment。
- 10 年答法：**「个人项目 Docker 落地；K8s 懂概念和与微服务关系，等待业务规模再上」**。

================

---

## 题 7：配置、密钥与 twelve-factor

================

## 面试问题：

容器里配置和密钥怎么管理？什么是 12-Factor？你们 env.sh 怎么设计？

## 考察点：

- 配置外置、不进镜像
- 与 Nacos、宝塔实践一致
- 安全面试点

## 标准答案：

**12-Factor（与容器相关几条）**：

- **III 配置**：存环境变量，不硬编码
- **VI 进程**：无状态，状态放 DB/Redis
- **XI 日志**：stdout，由平台收集
- **XII 管理进程**：一次性任务与长期服务分离

**进销存做法**：

```bash
# /opt/inventory-ms/env.sh（服务器，不进 Git）
export SPRING_PROFILES_ACTIVE=prd
export DB_URL=jdbc:postgresql://127.0.0.1:5432/...
export DB_PWD=***
export NACOS_ADDR=127.0.0.1:8848
export JWT_SECRET=***
export DASHSCOPE_API_KEY=***
export JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
```

```bash
docker run --env-file /opt/inventory-ms/env.sh ...
```

**Nacos Config** 可叠加：非敏感配置放 Nacos；**密钥仍环境变量或 Secret**。

**K8s 对应**：ConfigMap + **Secret**（base64，配合 RBAC）。

## 通俗理解：

镜像像 **标准化餐盒**；密码和数据库地址像 **各店当天的供应商电话**，写在 **店长抽屉（env.sh）**，不印在餐盒包装上。

## 项目结合：

- Gitee CD **不传密钥**，服务器预置 env.sh
- 本地 dev：`application-dev.yml` + 本地密码（gitignore）
- AI Key 缺失 → **降级固定话术**（不拖垮启动）

## 面试官追问：

1. 密钥进 Nacos 明文行吗？
2. ConfigMap 热更新 Java 能感知吗？
3. .env 文件权限？

## 高级回答：

- Nacos **加密插件** 或只存非敏感；高敏感 **Vault / 云 KMS**。
- **@RefreshScope** 部分 Bean 可刷新；DataSource 热更慎用。
- `chmod 600 env.sh`，仅 root/部署用户可读。
- 10 年答法：**「12-Factor 我实践在 env.sh + profile；镜像零密钥」**。

================

---

## 题 8：云原生全景口述 — 从开发到 nestparts.top

================

## 面试问题：

请从开发者 push 代码到用户在 nestparts.top 下单，讲一遍完整链路。云原生在你项目里落地到哪一步？和生产级差距？

## 考察点：

- 端到端串联：Git → CI → Docker → Nginx → Gateway → 微服务
- 诚实完成度
- 10 年开发者「能落地、知边界」

## 标准答案：

**完整链路（口述脚本）**：

```text
1. 开发 push ms/dev → Gitee Order-CI Maven 编译
2. 发版：手动 Order-CD → 主机组 Agent 执行 deploy-order-docker.sh
3. 服务器：新 jar → docker build → order-c 重启（host 网络 + prd profile + env.sh）
4. 用户访问 nestparts.top → 宝塔 Nginx 443 TLS
5. 反代 → Gateway:8080 → JWT → 路由
6. 下单 POST /api/orders → order-service（容器或 jar）
7. Feign → Nacos 发现 inventory → V4 原子锁库存
8. 返回 JSON；前端展示

旁路：Redis 登录态、Sentinel 限流、PG 真相库、AI 调 DashScope HTTPS
```

**已落地云原生**：

| ✅ | 项 |
|----|-----|
| | order Dockerfile + 本机/服务器容器 |
| | 五服务 compose 本机练习 |
| | Gitee Go CI + 手动 CD |
| | 部署脚本 + 探活 |

**未做 / 口述即可**：

| 📖 | 项 |
|----|-----|
| | K8s 集群、Helm、HPA |
| | 全服务 GitOps 自动 CD |
| | 中间件容器化 / Service Mesh |
| | 集中日志 ELK、链路 SkyWalking 生产级 |

**与「真生产」差距**：多可用区、全链路监控、自动扩缩、密钥轮换、WAF、DB 主从 — **面试主动说差距不减分**。

## 通俗理解：

你现在像 **一家店通了「中央厨房配送 + 标准餐盒」**；大型连锁还要 **全国调度中心（K8s）、统一监控大屏、多仓库灾备**——你知道方向，个人项目把 **餐盒和配送流程** 跑通就值钱了。

## 项目结合：

- 里程碑 **v3.7.0**：容器 + CI/CD 文档收口
- 域名 **nestparts.top**，腾讯云轻量 + 宝塔
- 简历可写：**Docker / Compose、Gitee Go CI+手动 CD、生产可演示**

**30 秒电梯版**：

> 进销存从单体演进到 Spring Cloud，order 已 Docker 化；Gitee Go push 自动编译，CD 手动确认后脚本重建容器；Nginx 入口 Gateway，Nacos 发现，库存 V4 原子扣减。K8s 我懂 Pod/Deployment/滚动发布，个人环境用 Docker+Compose，规模上来再上 K8s。

## 面试官追问：

1. 为什么不全服务都容器化？
2. 蓝绿和滚动区别？
3. 你下一步云原生补什么？

## 高级回答：

- **不全容器化**：中厂单机、无 K8s 时 **一个 order 容器验证交付链** 够面试；其余 jar 降低运维面。
- **蓝绿**：两套环境切换；**滚动**：逐步替换实例，资源省。
- **下一步**：全服务 compose 生产化、镜像私有仓库、Prometheus 监控、可选 K3s 单机 K8s 练习。
- 10 年答法：**「能画全链路；强调 CI/CD 和 Docker 是真跑过的，K8s 是能力储备不是吹项目规模」**。

---

## 本批小结 · 全库完成

| 主题 | 要能脱口而出的点 |
|------|-----------------|
| Docker 三宝 | 镜像/容器/Dockerfile |
| Dockerfile | JRE 瘦镜像、多阶段、env 不进镜像 |
| 网络 | host 模式连宿主机 Nacos/PG |
| CI/CD | push CI、手动 CD、deploy 脚本 |
| 资源 | MaxRAMPercentage、OOMKilled |
| K8s 口述 | Pod/Deployment/Service/Ingress |
| 配置 | env.sh、12-Factor |
| 全链路 | nestparts.top → Nginx → Gateway → order 容器 |

---

## 全 10 批索引

| 批 | 模块 | 文档 |
|----|------|------|
| 1 | Java 基础 | [面试题-01-Java基础.md](./面试题-01-Java基础.md) |
| 2 | JVM | [面试题-02-JVM.md](./面试题-02-JVM.md) |
| 3 | 网络 | [面试题-03-网络.md](./面试题-03-网络.md) |
| 4 | MySQL/PostgreSQL | [面试题-04-MySQL与PostgreSQL.md](./面试题-04-MySQL与PostgreSQL.md) |
| 5 | Spring 全家桶 | [面试题-05-Spring全家桶.md](./面试题-05-Spring全家桶.md) |
| 6 | 并发（公共厕所） | [面试题-06-并发编程.md](./面试题-06-并发编程.md) |
| 7 | Redis | [面试题-07-Redis.md](./面试题-07-Redis.md) |
| 8 | Elasticsearch | [面试题-08-Elasticsearch.md](./面试题-08-Elasticsearch.md) |
| 9 | 微服务（Eureka） | [面试题-09-微服务架构.md](./面试题-09-微服务架构.md) |
| 10 | 云原生 | 本文 |

总入口：[面试题-中高级Java后端-总索引.md](./面试题-中高级Java后端-总索引.md)
