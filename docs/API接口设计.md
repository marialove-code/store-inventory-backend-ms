# API 接口设计

> 基础路径：`http://{host}:8080/api`  
> 认证方式：`Authorization: Bearer {accessToken}`  
> 文档地址：`/api/doc.html`（Knife4j）  
> 统一响应：`Result<T>` → `{ "code": 200, "message": "操作成功", "data": {} }`

---

## 1. 通用约定

### 1.1 请求头

| Header | 说明 |
|--------|------|
| `Authorization` | `Bearer {accessToken}`，除白名单接口外必填 |
| `Content-Type` | `application/json`（POST/PUT 请求体） |

### 1.2 状态码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 500 | 业务失败 |
| 1101 | Token 无效 |
| 1102 | Token 过期（前端自动刷新） |
| 1103 | Token 为空 |
| 1201 | 无权限访问 |
| 1304 | 请求过于频繁（限流） |

### 1.3 分页参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `pageNum` | int | 页码，默认 1 |
| `pageSize` | int | 每页条数，默认 10 |

分页响应格式（MyBatis-Plus `Page`）：

```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 1.4 白名单（无需 Token）

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/refreshToken`
- `GET /upload/avatar/**`、`/upload/product/**`、`/upload/brand/**`
- Swagger 相关：`/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`

---

## 2. 认证模块 `/auth`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/auth/register` | 公开（限流 5次/60s） | 用户注册 |
| POST | `/auth/login` | 公开 | 登录，返回双 Token |
| POST | `/auth/logout` | 需登录 | 登出，清除 Redis 登录态 |
| POST | `/auth/refreshToken` | 公开 | 刷新 AccessToken |
| GET | `/auth/current` | 需登录 | 获取当前登录用户信息 |

**登录响应示例**：

```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 7200
  }
}
```

---

## 3. 系统管理

### 3.1 用户管理 `/sysUser`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/sysUser/list` | `system:user:list` | 用户分页列表 |
| GET | `/sysUser/user/{id}` | `system:user:edit` | 用户详情（编辑回显） |
| PUT | `/sysUser/user/{id}` | `system:user:edit` | 修改用户 |
| PUT | `/sysUser/{id}/status` | `system:user:changeStatus` | 修改用户状态 |
| PUT | `/sysUser/{id}/resetPassword` | `system:user:resetPwd` | 重置密码 |
| DELETE | `/sysUser/{id}` | `system:user:delete` | 删除用户 |
| DELETE | `/sysUser/batch` | `system:user:batchDelete` | 批量删除 |
| GET | `/sysUser/{userId}/roleIds` | `system:user:assign` | 查询已分配角色 ID |
| POST | `/sysUser/{userId}/role` | `system:user:assign` | 保存用户角色分配 |
| POST | `/sysUser/uploadAvatar` | 需登录 | 上传头像 |

### 3.2 角色管理 `/sysRole`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/sysRole/listAll` | 无 | 所有正常角色（下拉） |
| GET | `/sysRole/list` | `system:role:list` | 角色分页列表 |
| GET | `/sysRole/{id}` | `system:role:list` | 角色详情 |
| POST | `/sysRole` | `system:role:add` | 新增角色 |
| PUT | `/sysRole/{id}` | `system:role:edit` | 修改角色 |
| DELETE | `/sysRole/{id}` | `system:role:delete` | 删除角色 |
| DELETE | `/sysRole/batch` | `system:role:batchDelete` | 批量删除 |
| PUT | `/sysRole/{id}/status` | `system:role:changeStatus` | 修改角色状态 |
| GET | `/sysRole/permission/tree` | `system:role:assign` | 权限树 |
| GET | `/sysRole/{roleId}/permissionIds` | `system:role:assign` | 角色已有权限 ID |
| POST | `/sysRole/{roleId}/permission` | `system:role:assign` | 保存角色权限 |

### 3.3 权限管理 `/sysPermission`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/sysPermission/list` | `system:permission:list` | 权限分页列表 |
| GET | `/sysPermission/tree` | `system:permission:list` | 权限树 |
| GET | `/sysPermission/{id}` | `system:permission:list` | 权限详情 |
| POST | `/sysPermission` | `system:permission:add` | 新增权限 |
| PUT | `/sysPermission/{id}` | `system:permission:edit` | 修改权限 |
| DELETE | `/sysPermission/{id}` | `system:permission:delete` | 删除权限 |
| PUT | `/sysPermission/{id}/status` | `system:permission:changeStatus` | 修改权限状态 |
| GET | `/sysPermission/listAllPermCodes` | 需登录 | 所有权限码 |

### 3.4 菜单管理 `/system/menu`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/system/menu/all` | 需登录 | 当前用户菜单树 |
| GET | `/system/menu/list` | `system:menu:list` | 菜单管理列表 |
| POST | `/system/menu` | `system:menu:add` | 新增菜单 |
| PUT | `/system/menu/{id}` | `system:menu:edit` | 修改菜单 |
| DELETE | `/system/menu/{id}` | `system:menu:delete` | 删除菜单 |
| PUT | `/system/menu/{id}/status` | `system:menu:changeStatus` | 修改菜单状态 |

### 3.5 在线用户 `/sys/online`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/sys/online/list` | `system:online:list` | 在线用户分页 |
| DELETE | `/sys/online/{tokenKey}` | `system:online:forceLogout` | 强制下线 |
| GET | `/sys/online/redis/info` | 需登录 | Redis 监控信息 |
| GET | `/sys/online/redis/page` | `system:online:redis:list` | Redis Key 分页 |
| DELETE | `/sys/online/redis/del` | `system:online:redis:delete` | 删除 Redis Key |

### 3.6 登录日志 `/system/login/log`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/system/login/log/list` | `system:loginlog:list` | 登录日志分页 |
| GET | `/system/login/log/export` | `system:loginlog:export` | 导出 Excel |
| DELETE | `/system/login/log/clear` | `system:loginlog:clear` | 清空日志 |

### 3.7 操作日志 `/system/operate/log`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/system/operate/log/list` | `system:operlog:list` | 操作日志分页 |
| GET | `/system/operate/log/export` | `system:operlog:export` | 导出 Excel |

---

## 4. 个人中心

### 4.1 个人主页 `/profile`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/profile/overview` | 个人概览 |
| GET | `/profile/kick-others` | 踢出其他设备 |

### 4.2 我的日志 `/system/login/mylog`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/login/mylog/loglist` | 当前用户登录记录 |
| GET | `/system/login/mylog/logExport` | 导出登录日志 |
| GET | `/system/login/mylog/operList` | 当前用户操作日志 |
| GET | `/system/login/mylog/operExport` | 导出操作日志 |

---

## 5. 商品管理

### 5.1 商品品牌 `/goods/brand`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/goods/brand/list` | `goods:brand:list` | 品牌分页 |
| GET | `/goods/brand/listAll` | 无 | 全部品牌（下拉） |
| POST | `/goods/brand` | `goods:brand:add` | 新增品牌 |
| PUT | `/goods/brand/{id}` | `goods:brand:edit` | 修改品牌 |
| DELETE | `/goods/brand/{id}` | `goods:brand:delete` | 删除品牌 |
| DELETE | `/goods/brand/batch` | `goods:brand:batchDelete` | 批量删除 |
| PUT | `/goods/brand/{id}/status` | `goods:brand:changeStatus` | 修改状态 |
| POST | `/goods/brand/uploadLogo` | `goods:brand:edit` | 上传 Logo |

### 5.2 商品分类 `/goods/category`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/goods/category/tree` | `goods:category:list` | 分类树 |
| POST | `/goods/category` | `goods:category:add` | 新增分类 |
| PUT | `/goods/category/{id}` | `goods:category:edit` | 修改分类 |
| DELETE | `/goods/category/{id}` | `goods:category:delete` | 删除分类 |
| DELETE | `/goods/category/batch` | `goods:category:batchDelete` | 批量删除 |
| PUT | `/goods/category/{id}/status` | `goods:category:changeStatus` | 修改状态 |
| PUT | `/goods/category/batch/status` | `goods:category:batchStatus` | 批量修改状态 |

### 5.3 商品信息 `/goods/product`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/goods/product/list` | `goods:product:list` | 商品分页 |
| POST | `/goods/product` | `goods:product:add` | 新增商品 |
| PUT | `/goods/product/{id}` | `goods:product:edit` | 编辑商品 |
| DELETE | `/goods/product/{id}` | `goods:product:delete` | 删除商品 |
| DELETE | `/goods/product/batch` | `goods:product:batchDelete` | 批量删除 |
| PUT | `/goods/product/{id}/shelf` | `goods:product:changeShelf` | 单个上下架 |
| PUT | `/goods/product/batch/shelf` | `goods:product:batchShelf` | 批量上下架 |
| POST | `/goods/product/uploadImage` | `goods:product:edit` | 上传商品图片 |

---

## 6. 库存管理

### 6.1 库存列表 `/inventory/stock`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/inventory/stock/list` | `inventory:stock:list` | 库存分页 |
| PUT | `/inventory/stock/list/{id}/stockWarn` | `inventory:stock:edit` | 修改预警阈值 |

### 6.2 入库管理 `/inventory/stockin`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/inventory/stockin/list` | `inventory:stockin:list` | 入库单分页 |
| POST | `/inventory/stockin` | `inventory:stockin:add` | 新增入库单 |

### 6.3 出库管理 `/inventory/stockout`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/inventory/stockout/list` | `inventory:stockout:list` | 出库单分页 |
| POST | `/inventory/stockout` | `inventory:stockout:add` | 新增出库单 |

### 6.4 库存预警 `/inventory/warn`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/inventory/warn/list` | `inventory:warn:list` | 预警列表 |
| GET | `/inventory/warn/{id}` | `inventory:warn:list` | 预警详情 |
| PUT | `/inventory/warn/{id}/stockWarn` | `inventory:warn:edit` | 修改预警阈值 |

### 6.5 库存流水 `/inventory/flow`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/inventory/flow/list` | `inventory:flow:list` | 流水分页 |
| POST | `/inventory/flow/export` | `inventory:flow:export` | 导出 Excel |

---

## 7. 订单管理

### 7.1 订单列表 `/order/info`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/order/info/list` | `order:info:list` | 订单分页 |
| POST | `/order/info` | `order:info:add` | 新建订单 |
| PUT | `/order/info/{id}/pay` | `order:info:pay` | 确认支付 |
| PUT | `/order/info/{id}/receive` | `order:info:receive` | 确认收货 |
| PUT | `/order/info/{id}/cancel` | `order:info:cancel` | 取消订单 |

### 7.2 发货管理 `/order/delivery`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/order/delivery/list` | `order:delivery:list` | 待发货订单分页 |
| PUT | `/order/delivery/{id}` | `order:delivery:delivery` | 确认发货 |

### 7.3 退款管理 `/order/refund`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/order/refund/list` | `order:refund:list` | 退款单分页 |
| POST | `/order/refund` | `order:refund:apply` | 发起退款申请 |
| PUT | `/order/refund/{id}/approve` | `order:refund:approve` | 通过退款 |
| PUT | `/order/refund/{id}/reject` | `order:refund:reject` | 拒绝退款 |

---

## 8. 系统监控

### 8.1 API 监控 `/monitor`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/monitor/api` | `monitor:api:list` | API 性能监控 |

### 8.2 服务监控 `/monitor/server`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/monitor/server` | `monitor:server:view` | 服务器监控 |

### 8.3 Redis 监控 `/monitor/redis`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/monitor/redis/info` | `monitor:redis:view` | Redis 基础指标 |
| GET | `/monitor/redis/bigkey` | 需登录 | 大 Key 分页 |
| GET | `/monitor/redis/trend` | 需登录 | 趋势图数据 |

---

## 9. 看板

### 9.1 管理端看板 `/dashboard`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/index?period=7d` | 首页聚合数据（7d/30d/90d） |

### 9.2 门店看板 `/shop/dashboard`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/dashboard/overview` | 门店看板数据 |

---

## 10. 门店收银

### 10.1 门店商品 `/shop/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/product/list` | 配件商品分页 |
| GET | `/shop/product/options` | 开单下拉选项 |
| POST | `/shop/product` | 新增配件商品 |
| PUT | `/shop/product/{id}` | 修改（补货+调价） |

### 10.2 销售记录 `/shop`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/shop/record/list` | 销售流水列表 |
| POST | `/shop/sale` | 开单记账 |
| GET | `/shop/sale/stats` | 今日/本月营收统计 |

---

## 11. 接口设计规范

| 规范 | 说明 |
|------|------|
| RESTful 风格 | GET 查询、POST 新增、PUT 修改、DELETE 删除 |
| 路径命名 | 小写 + 驼峰，资源名复数或语义化 |
| 权限粒度 | 一个按钮操作对应一个 `perm_code` |
| 幂等性 | 删除、状态变更使用 PUT/DELETE |
| 文件上传 | `multipart/form-data`，单文件最大 10MB |
| 错误处理 | 业务异常返回 `code != 200`，附带 `message` |
| 操作审计 | 增删改接口标注 `@OperationLog` |
