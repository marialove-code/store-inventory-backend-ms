# Order 服务 CI/CD（中厂实用版）

> 适用：Gitee 仓库 + 腾讯云轻量/宝塔单机，**无专职运维**。  
> 范围：只自动化 **order-service**（其它服务同理可抄）。  
> 原则：编译机出 **jar**，服务器 **Docker 重建容器**；不上私有镜像仓库、不上 Jenkins、不上 K8s。

---

## 1. 和「有运维 / 大厂」差在哪

| | 大厂 / 有运维 | 本方案（中厂常见） |
|--|----------------|-------------------|
| 编译 | 专用构建集群 | Gitee Go 云端 Maven |
| 产物 | 镜像 + Harbor/ACR | **jar**（几十 MB） |
| 发布 | K8s / 发布平台 / 灰度 | **SSH/Agent + 一键脚本** 换包重启 |
| 触发 | 多环境晋升 | push 只 CI；**上线要打 tag 或手动** |

面试可说：

> 我们没有独立运维，用 Gitee Go 做 CI，产物是 jar；CD 用主机 Agent 把包丢到机器，脚本 `docker build` 重建 order 容器并 curl 探活。现网和家里门店同一台机，所以 **push 不自动上生产**，部署阶段手动确认。

---

## 2. 流程一眼看懂

```text
push ms/dev  ──►  Order-CI：mvn package（只验证能编译）
打 tag order-v* 或手动跑 CD
        ──►  Order-CD：mvn package
                ──►  主机下载 jar
                ──►  deploy-order-docker.sh
                        · docker build
                        · 重启 order-c（--network host + prd）
                        · curl /api/order/ping
```

仓库内关键文件：

| 路径 | 作用 |
|------|------|
| [`.workflow/order-ci.yml`](../.workflow/order-ci.yml) | push `ms/dev` → 只编译 |
| [`.workflow/order-cd.yml`](../.workflow/order-cd.yml) | tag `order-v*` → 编译 + **手动确认**部署 |
| [`deploy/order-runtime/Dockerfile`](../deploy/order-runtime/Dockerfile) | 服务器上只靠「Dockerfile + jar」出镜像 |
| [`scripts/deploy-order-docker.sh`](../scripts/deploy-order-docker.sh) | 换 jar、重建容器、探活 |

---

## 3. 服务器一次性准备（先手工跑通）

### 3.1 目录与文件

在服务器执行（路径可按你实际调整，需与脚本默认一致）：

```bash
MS_HOME=/www/wwwroot/inventory-ms-backend
mkdir -p $MS_HOME/deploy/order-runtime $MS_HOME/scripts

# 从本机/仓库拷过去（宝塔上传亦可）：
#   deploy/order-runtime/Dockerfile
#   scripts/deploy-order-docker.sh
chmod +x $MS_HOME/scripts/deploy-order-docker.sh
```

确认已有：

- Docker 可用：`docker version`
- 环境变量：`/opt/inventory-ms/env.sh`（含 `DB_PWD` 等，**不要进 Git**）
- 当前 order 已是容器 `order-c`（你已跑通）

### 3.2 用现有 jar 试跑脚本

把当前线上同版本 jar 放到 runtime 目录（或指向你刚上传的包）：

```bash
# 示例：若 jars 目录已有包
cp /www/wwwroot/inventory-ms-backend/jars/order-service-3.6.0.jar \
   /www/wwwroot/inventory-ms-backend/deploy/order-runtime/order-service-3.6.0.jar

bash /www/wwwroot/inventory-ms-backend/scripts/deploy-order-docker.sh
```

成功标志：

- 脚本打印「探活成功」
- `curl -s http://127.0.0.1:8083/api/order/ping` 返回 `code:200`

**这一步不通，不要接 Gitee Go。** 流水线只是远程调用同一脚本。

### 3.3 回滚

脚本覆盖前会把旧 jar 存为 `order-service-3.6.0.jar.prev`：

```bash
bash /www/wwwroot/inventory-ms-backend/scripts/deploy-order-docker.sh \
  /www/wwwroot/inventory-ms-backend/deploy/order-runtime/order-service-3.6.0.jar.prev
```

---

## 4. 开通 Gitee Go 与主机

1. 打开仓库 → **流水线 / Gitee Go** → 按提示开通（注意免费构建分钟数）。  
2. 推送含 `.workflow/` 的提交后，应能看到两条流水线：**Order-CI编译**、**Order-CD部署**。  
3. **计算资源** → 添加主机组 → 按页面脚本在服务器装 Agent（或按官方文档配 SSH 主机）。  
4. 记下 **主机组 ID**，打开 [`.workflow/order-cd.yml`](../.workflow/order-cd.yml)：  
   - 把 `HOST_GROUP_ID` / `hostGroupID` 里的 `REPLACE_WITH_YOUR_HOST_GROUP_ID` **改成真实 ID**  
   - 再提交推送  
5. 凭证、主机密码 **只放 Gitee Go 控制台**，不要写进仓库。

> 若可视化编辑器与 YAML 字段名略有差异，以 Gitee 页面为准；保持「Maven 出 jar → 主机部署执行脚本」即可。

---

## 5. 日常怎么用

### 只验证能不能编过（CI）

```text
改代码 → push 到 ms/dev → 看 Order-CI 是否绿
```

不会动服务器上的容器。

### 要上线（CD）

**方式 A（推荐）：打 tag**

```bash
git tag order-v3.6.1
git push origin order-v3.6.1
```

流水线跑编译后，**部署阶段需在 Gitee Go 点一次确认**（YAML 里 `trigger: manual`），再发到机器。

**方式 B：在 Gitee Go 手动运行 Order-CD**

适合临时发版、不想打 tag。

### 发版后自检

```bash
docker ps | grep order-c
curl -s http://127.0.0.1:8083/api/order/ping
# 若网关仍在：curl -s http://127.0.0.1:9080/api/order/ping
```

---

## 6. 注意点（少踩坑）

1. **版本号**：当前 jar / 镜像标签写死为 `3.6.0`，与父 POM 一致。升版本时要同步改：`pom`、`Dockerfile` COPY 名、脚本里的 `JAR_NAME`、流水线 artifacts 路径。  
2. **无 RabbitMQ**：服务器可能仍刷 MQ 连接失败日志；**ping 通即视为本次部署成功**（与现网一致）。  
3. **2 核 4G**：不要在服务器上跑完整 `mvn`；编译放 Gitee Go。  
4. **脚本要先在机器上**：Agent 只负责下 jar + 调脚本；`deploy-order-docker.sh` 与 runtime `Dockerfile` 需已存在于 `MS_HOME`（可用宝塔上传或 `git pull` 一次）。  
5. **不要**把 `/opt/inventory-ms/env.sh` 提交到 Git。

---

## 7. 面试口述（约 5 句）

1. CI：Gitee Go，JDK17，只编 order 模块出 jar。  
2. CD：主机组 Agent 下发 jar，服务器脚本重建 Docker 容器。  
3. 中间件（Nacos/PG/Redis）仍在宿主机，`--network host` + `prd`。  
4. 现网=家用机，所以 push 只 CI，部署要 tag/手动确认。  
5. 不做镜像仓库和 K8s：单机体量用 jar+脚本就够，灰度与多副本用并发设计口述。

---

## 8. 验收清单

- [ ] 服务器手动执行 `deploy-order-docker.sh` 探活成功  
- [ ] push `ms/dev` → Order-CI 绿  
- [ ] 主机组已绑定，`order-cd.yml` 已改 `hostGroupID`  
- [ ] tag `order-v*` 或手动 CD → 确认部署 → ping `code:200`  
- [ ] 能讲清 CI/CD 边界与密钥位置  
