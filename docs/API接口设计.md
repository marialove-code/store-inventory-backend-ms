# API 接口设计

> [← 返回 README](../README.md)

**基础路径：** `http://localhost:8080/api`  
**在线调试：** `/api/doc.html`（Knife4j，最全最准，联调优先看这个）

---

## 通用约定

### 统一响应

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

### 常用 code

| code | 含义 |
|------|------|
| 200 | 成功 |
| 1102 | Token 过期 |
| 1201 | 无权限 |
| 1304 | 请求太频繁 |

### 请求头

```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 分页

查询参数：`pageNum`、`pageSize`（默认 1、10）

### 不用 Token 的接口

- `POST /auth/login`、`/auth/register`、`/auth/refreshToken`
- 静态图片：`/upload/**`
- Swagger / Knife4j 文档路径

---

## 模块接口速查

### 认证 `/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 注册 |
| POST | `/auth/login` | 登录 |
| POST | `/auth/logout` | 登出 |
| POST | `/auth/refreshToken` | 刷新 Token |
| GET | `/auth/current` | 当前用户信息 |

### 系统管理

| 前缀 | 说明 |
|------|------|
| `/sysUser` | 用户 CRUD、分配角色、重置密码 |
| `/sysRole` | 角色 CRUD、分配权限 |
| `/sysPermission` | 权限树维护 |
| `/system/menu` | 当前用户菜单、菜单管理 |
| `/sys/online` | 在线用户、强制下线 |
| `/system/login/log` | 登录日志 |
| `/system/operate/log` | 操作日志 |

### 商品 `/goods`

| 前缀 | 说明 |
|------|------|
| `/goods/brand` | 品牌 |
| `/goods/category` | 分类树 |
| `/goods/product` | 商品 CRUD、上下架、传图 |

### 库存 `/inventory`

| 前缀 | 说明 |
|------|------|
| `/inventory/stock` | 库存列表、改预警值 |
| `/inventory/stockin` | 入库单 |
| `/inventory/stockout` | 出库单 |
| `/inventory/warn` | 预警列表 |
| `/inventory/flow` | 流水查询、导出 |

### 订单 `/order`

| 前缀 | 说明 |
|------|------|
| `/order/info` | 订单 CRUD、支付、收货、取消 |
| `/order/delivery` | 发货 |
| `/order/refund` | 退款申请、审核 |

### 门店 `/shop`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/product/list` | 门店商品 |
| GET | `/shop/product/options` | 开单下拉 |
| POST | `/shop/sale` | **开单记账** |
| GET | `/shop/record/list` | 销售流水 |
| GET | `/shop/sale/stats` | 今日/本月统计 |
| GET | `/shop/dashboard/overview` | 门店看板 |

### 看板 & 监控

| 前缀 | 说明 |
|------|------|
| `/dashboard/index` | 管理端首页 |
| `/monitor/api` | 接口监控 |
| `/monitor/server` | 服务器监控 |
| `/monitor/redis` | Redis 监控 |

### 个人 `/profile`

个人概览、踢其他设备、我的日志查询导出。

---

## REST 习惯

| 操作 | 方法 |
|------|------|
| 列表 / 详情 | GET |
| 新增 | POST |
| 修改 | PUT |
| 删除 | DELETE |

权限码与 `@RequiresPerm` 对应关系见 [RBAC 权限设计](RBAC权限设计.md)。

---

## 相关文档

- [后端功能模块](后端功能模块.md)
- [RBAC 权限设计](RBAC权限设计.md)
