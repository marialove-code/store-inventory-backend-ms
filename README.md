# 门店简易记账 · 后端（微服务学习仓）

> ⚠️ **本目录是微服务拆分学习副本（`store-inventory-backend-ms`）**  
> - 分支 `ms/dev`，远程 Gitee：`store-inventory-backend-ms`  
> - **不要**与家里在用的单体仓 `store-inventory-backend`（8080）混用  
> - 业务从单体**渐进迁入**，本仓从 0 搭多模块骨架  

## 当前结构（P4a：AI 独立服务已完成）

```text
store-inventory-backend-ms/          # 父 POM（packaging=pom）
├── inventory-common                 # 公共 jar：Result / 枚举 / 单号工具
├── inventory-service                # 库存服务  端口 8082
├── order-service                    # 订单服务  端口 8083（HTTP 调库存）
└── ai-service                       # AI 服务    端口 8084
```

| 服务 | 端口 | 探活 |
|------|------|------|
| inventory-service | 8082 | `GET http://localhost:8082/api/inventory/ping` |
| order-service | 8083 | `GET http://localhost:8083/api/order/ping` |
| ai-service | 8084 | `GET http://localhost:8084/api/ai/ping` |
| 单体（参考，勿改） | 8080 | 家庭在用 |

### ai-service 能力

| 能力 | 路径 |
|------|------|
| 探活 | `GET /ai/ping` |
| 智能客服 | `POST /ai/chat` |
| 商品智搜 | `POST /ai/product/parse` |
| Text-to-SQL | `POST /ai/sql/query` |
| 运维分析 | `GET /ai/ops/analyze` |
| 看板洞察 | `GET /ai/dashboard/insight` |
| 销售预测 | `GET /ai/dashboard/sales-forecast` |
| 补货建议 | `GET /ai/inventory/replenish` |

需环境变量：`DB_PWD`、`DASHSCOPE_API_KEY`（无 Key 时走规则降级，接口仍可用）。

## 本地启动

```powershell
cd e:\Projects\store-inventory-backend-ms
$env:DB_PWD="你的库密码"
$env:DASHSCOPE_API_KEY="你的通义 Key"   # 可选
mvn clean install -DskipTests

# 库存 8082 / 订单 8083 / AI 8084 分别 spring-boot:run
```

## 阶段说明

- **P0～P3**：库存菜单闭环 + 订单 HTTP 调库存  
- **P4a（当前）**：`ai-service` 独立（同库直连，暂无鉴权/限流/Redis 会话）  
- **P4b**：`platform-service`（认证 + 商品 + 系统）  
- **P5+**：Feign → Nacos → Gateway  

详见 [docs/微服务拆分-起步.md](docs/微服务拆分-起步.md)
