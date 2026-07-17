# 本机启动命令（Nacos / Sentinel）

> Windows + PowerShell / CMD 可直接复制。  
> 路径按你当前机器：`E:\nacos-server-2.5.2`、`E:\tools\sentinel`、JDK 17。若目录不同只改路径即可。

**建议启动顺序：** Nacos →（可选）Sentinel Dashboard → 业务服务 → Gateway。

---

## 1. Nacos（端口 8848）

控制台：http://127.0.0.1:8848/nacos  
默认账号密码：`nacos` / `nacos`

### CMD（推荐整段复制）

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.19
set CUSTOM_NACOS_MEMORY=-Xms512m -Xmx512m -Xmn256m --add-opens=java.base/java.io=ALL-UNNAMED
cd /d E:\nacos-server-2.5.2\nacos\bin
startup.cmd -m standalone
```

### PowerShell

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.19"
$env:CUSTOM_NACOS_MEMORY = "-Xms512m -Xmx512m -Xmn256m --add-opens=java.base/java.io=ALL-UNNAMED"
Set-Location E:\nacos-server-2.5.2\nacos\bin
.\startup.cmd -m standalone
```

说明：

- `-m standalone`：单机模式（学习用）
- `CUSTOM_NACOS_MEMORY` 里的 `--add-opens=...`：JDK 17 下避免 `InaccessibleObjectException`
- 停止：同目录执行 `shutdown.cmd`（或关掉对应 Java 进程）

---

## 2. Sentinel Dashboard（端口 8858）

控制台：http://127.0.0.1:8858  
默认账号密码：`sentinel` / `sentinel`

> **不要用 8080**：会和 Gateway 冲突。本项目约定 **8858**。

### CMD（推荐整段复制）

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.19
cd /d E:\tools\sentinel
java -Dserver.port=8858 -jar sentinel-dashboard-1.8.6.jar
```

### PowerShell

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.19"
Set-Location E:\tools\sentinel
java -Dserver.port=8858 -jar sentinel-dashboard-1.8.6.jar
```

说明：

- jar 下载：https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar
- 窗口保持打开；关掉窗口即停止 Dashboard
- 业务侧：`order-service` 已配置 `dashboard: 127.0.0.1:8858`；控制台左侧要看到应用，需先访问几次接口（如 `/api/order/ping`）

---

## 3. 端口速查（避免混）

| 进程 | 端口 |
|------|------|
| Nacos | **8848** |
| Sentinel Dashboard | **8858** |
| Gateway | 8080 |
| platform / inventory / order / ai | 8081 / 8082 / 8083 / 8084 |
| 家里单体（勿混） | 8080（另一套工程） |

---

## 4. 相关笔记

- 治理层：[`治理层接入笔记-Nacos-Feign-Gateway.md`](./治理层接入笔记-Nacos-Feign-Gateway.md)
- Sentinel：[`Sentinel最小限流-五步学习.md`](./Sentinel最小限流-五步学习.md)
