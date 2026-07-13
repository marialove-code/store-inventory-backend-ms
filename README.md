# 门店简易记账 · 后端（微服务学习仓）

> ⚠️ **本目录是微服务拆分学习副本（`store-inventory-backend-ms`）**  
> - 分支 `ms/dev`，远程 Gitee：`store-inventory-backend-ms`  
> - **不要**与家里在用的单体仓 `store-inventory-backend`（8080）混用  
> - 业务从单体**渐进迁入**，本仓从 0 搭多模块骨架  

## 当前结构（P3 已完成）

```text
store-inventory-backend-ms/          # 父 POM（packaging=pom）
├── inventory-common                 # 公共 jar：Result / 枚举 / 单号工具
├── inventory-service                # 库存服务  端口 8082  context-path=/api
└── order-service                    # 订单服务  端口 8083  context-path=/api（HTTP 调库存）
```

| 服务 | 端口 | 探活 |
|------|------|------|
| inventory-service | 8082 | `GET http://localhost:8082/api/inventory/ping` |
| order-service | 8083 | `GET http://localhost:8083/api/order/ping` |
| 单体（参考，勿改） | 8080 | 家庭在用 |

### inventory-service 菜单闭环

| 菜单 | 接口前缀 |
|------|----------|
| 库存列表 | `/inventory/stock` |
| 入库管理 | `/inventory/stockin` |
| 出库管理 | `/inventory/stockout` |
| 库存预警 | `/inventory/warn` |
| 库存流水 | `/inventory/flow` |
| 内部命令 | `/inventory/internal/**` |

## 本地启动

```powershell
cd e:\Projects\store-inventory-backend-ms
$env:DB_PWD="你的库密码"
mvn clean install -DskipTests

# 终端 1（先库存）
cd inventory-service
mvn spring-boot:run

# 终端 2（再订单）
cd order-service
mvn spring-boot:run
```

## 阶段说明

- **P0**：父子工程 + 两空服务可启动  
- **P1**：迁库存核心读写 + `/inventory/internal/**`  
- **P2**：迁订单正式业务，RestTemplate 调库存内部 API  
- **P3（当前）**：补全入库 / 出库 / 流水 / 预警  
- **P4+**：platform-service → Feign → Nacos  

详见 [docs/微服务拆分-起步.md](docs/微服务拆分-起步.md)
