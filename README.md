# 门店简易记账 · 后端（微服务学习仓）

> ⚠️ **本目录是微服务拆分学习副本（`store-inventory-backend-ms`）**  
> - 分支 `ms/dev`，远程 Gitee：`store-inventory-backend-ms`  
> - **不要**与家里在用的单体仓 `store-inventory-backend`（8080）混用  

## 当前结构（**v3.2.0** · 同机生产切流）

> 标签 `v3.2.0`：在 v3.1.0 治理层基础上完成 dev/prd 配置拆分、同机部署脚本与域名 Nginx 切到微服务前端 + Gateway。

```text
store-inventory-backend-ms/
├── inventory-common                 # 公共 jar：Result / ResultCode / RedisConstants / StockInitRequest …
├── platform-service                 # 平台服务  端口 8081（认证+商品+系统+看板+监控+门店）
├── inventory-service                # 库存服务  端口 8082
├── order-service                    # 订单服务  端口 8083（HTTP 调库存）
└── ai-service                       # AI 服务    端口 8084
```

| 服务 | 端口 | 探活 |
|------|------|------|
| platform-service | 8081 | `GET http://localhost:8081/api/platform/ping` |
| inventory-service | 8082 | `GET http://localhost:8082/api/inventory/ping` |
| order-service | 8083 | `GET http://localhost:8083/api/order/ping` |
| ai-service | 8084 | `GET http://localhost:8084/api/ai/ping` |
| 单体（参考，勿改） | 8080 | 家庭在用 |

### platform-service 范围（照搬单体）

认证 `/auth/**`、商品 `/goods/**`、系统用户角色权限日志在线、个人中心、看板 `/dashboard/**`、监控 `/monitor/**`、门店记账 `/shop/**`。  
保留 Security / JWT / Redis / `@RequiresPerm` / 限流 / 操作日志。

### 跨服务调用

| 调用方 | 被调 | 说明 |
|--------|------|------|
| platform → inventory | `InventoryStockClient` | 新增商品 `init-stock`；删除前查 `usable` |
| order → inventory | `InventoryStockClient` | lock / unlock / decrease-flow / increase / usable |

公共 DTO：`inventory-common` 中 `StockInitRequest`。

## 本地启动

```powershell
cd e:\Projects\store-inventory-backend-ms
$env:DB_PWD="你的库密码"
$env:REDIS_PWD="你的 Redis 密码"
$env:DASHSCOPE_API_KEY="可选"
mvn clean install -DskipTests

# 建议启动顺序：inventory → platform → order → ai
```

环境变量：`DB_PWD`、`REDIS_HOST`/`REDIS_PWD`、`JWT_SECRET`（可选覆盖）。

## 阶段说明

- **P0～P3**：库存菜单闭环 + 订单 HTTP 调库存  
- **P4a**：ai-service 独立  
- **P4（当前）**：platform-service（认证/商品/系统等照搬 + 跨服务库存 Client）  
- **P5+**：Feign → Nacos → Gateway  

详见 [docs/微服务拆分-起步.md](docs/微服务拆分-起步.md)  
生产同机切流与配置/Linux 命令：[docs/单体到微服务-部署与配置手册.md](docs/单体到微服务-部署与配置手册.md)
