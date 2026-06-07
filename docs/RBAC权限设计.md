# RBAC 权限设计

> 项目采用 **RBAC（Role-Based Access Control）** 模型：用户 → 角色 → 权限  
> 认证：JWT 双 Token + Redis 登录态  
> 授权：自定义 `@RequiresPerm` 注解 + AOP 切面

---

## 1. 权限模型

### 1.1 ER 关系

```
┌──────────┐     ┌──────────────┐     ┌──────────┐     ┌───────────────────┐     ┌────────────────┐
│ sys_user │────<│ sys_user_role │>────│ sys_role │────<│ sys_role_permission │>────│ sys_permission │
└──────────┘     └──────────────┘     └──────────┘     └───────────────────┘     └────────────────┘
   用户              用户角色关联          角色              角色权限关联              权限/菜单
```

- 一个用户可拥有多个角色
- 一个角色可拥有多个权限
- 用户最终权限 = 所有角色权限的并集

### 1.2 权限类型

| 类型 | 代码 | 说明 | 前端表现 |
|------|------|------|----------|
| 目录 | M | 侧边栏一级菜单 | 渲染导航分组 |
| 菜单 | C | 侧边栏二级页面 | 渲染路由页面 |
| 按钮 | F | 页面内操作 | 控制按钮显隐 |

### 1.3 权限码规范

格式：`模块:资源:操作`

```
system:user:list        # 系统管理 - 用户 - 列表查询
goods:product:add       # 商品管理 - 商品 - 新增
inventory:stockin:add   # 库存管理 - 入库 - 新增
order:info:pay          # 订单管理 - 订单 - 确认支付
monitor:redis:view      # 系统监控 - Redis - 查看
```

超级管理员通配符：`*:*:*`（`PermissionConstants.SUPER_PERM_CODE`）

---

## 2. 认证流程

### 2.1 双 Token 机制

| Token | 有效期 | 用途 |
|-------|--------|------|
| AccessToken | 120 分钟 | 接口鉴权，每次请求携带 |
| RefreshToken | 7 天 | AccessToken 过期后无感刷新 |

### 2.2 登录流程

```
用户提交账号密码
    │
    ▼
AuthServiceImpl.login()
    ├── 校验用户名/密码（BCrypt）
    ├── 校验账号状态（禁用则拒绝）
    ├── 查询用户角色列表
    ├── 查询用户权限码列表
    ├── 签发 AccessToken + RefreshToken（JwtUtil / HS256）
    ├── 写入 Redis 登录态
    │     ├── user:token:{userId}:access:{token}
    │     ├── user:token:{userId}:refresh:{token}
    │     └── user:device:{userId}（设备映射 Hash）
    ├── 缓存权限列表 user:perm:{userId}
    └── 记录登录日志（IP + 归属地）
    │
    ▼
返回 LoginTokenVO
```

### 2.3 请求鉴权流程

```
HTTP 请求
    │
    ▼
JwtAuthenticationFilter（Spring Security 过滤器链）
    ├── 白名单路径 → 直接放行
    ├── 解析 Authorization: Bearer {token}
    ├── 校验 JWT 签名与过期时间
    ├── Redis 校验登录态是否存在
    ├── 设置 LoginUserContext（ThreadLocal）
    └── 设置 SecurityContext Authentication
    │
    ▼
Spring Security（anyRequest.authenticated）
    ├── 未认证 → 返回 JSON { code: 1102 }
    └── 已认证 → 继续
    │
    ▼
PermissionAspect（仅 @RequiresPerm 方法触发）
    ├── SUPER_ADMIN 角色 → 直接放行
    ├── 查询用户 perm_code 列表（优先 Redis 缓存）
    ├── 比对 needPerm 是否在列表中
    └── 不匹配 → 抛出 PermissionException（code: 1201）
    │
    ▼
Controller → Service → 业务处理
```

---

## 3. 核心组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `SecurityConfig` | `config/security/` | 白名单、无状态 Session、JWT 过滤器链 |
| `JwtAuthenticationFilter` | `framework/security/filter/` | JWT + Redis 双重校验 |
| `JwtUtil` | `framework/security/jwt/` | Token 签发、解析、校验 |
| `@RequiresPerm` | `framework/security/permission/annotation/` | 方法级权限注解 |
| `PermissionAspect` | `framework/security/permission/aspect/` | AOP 权限校验 |
| `LoginUserContext` | `framework/security/context/` | ThreadLocal 当前用户 |
| `AuthServiceImpl` | `modules/auth/service/impl/` | 登录/注册/登出/刷新 |
| `SysPermissionService` | `modules/system/permission/service/` | 权限查询、菜单树、缓存管理 |
| `SysUserRoleService` | 同上 | 用户-角色关联 |
| `SysRolePermissionService` | 同上 | 角色-权限关联 |

---

## 4. Redis 缓存设计

| Key 模式 | 说明 | 失效时机 |
|----------|------|----------|
| `inventory:user:token:{userId}:access:{token}` | AccessToken 登录态 | 登出 / 过期 / 强制下线 |
| `inventory:user:token:{userId}:refresh:{token}` | RefreshToken | 登出 / 过期 |
| `inventory:user:perm:{userId}` | 用户权限码列表 | 角色变更 / 权限变更 / 登出 |
| `inventory:user:device:{userId}` | 设备 Token 映射 | 踢设备 / 登出 |

> Redis Key 前缀 `inventory:` 由 `spring.data.redis.key-prefix` 配置。

---

## 5. 超级管理员

| 属性 | 值 |
|------|-----|
| 角色码 | `SUPER_ADMIN` |
| 权限码 | `*:*:*` |
| 特权 | `PermissionAspect` 中直接放行所有 `@RequiresPerm` 接口 |
| 保护 | 不允许删除超级管理员角色/用户 |

---

## 6. 权限与菜单

### 6.1 菜单获取

```
GET /system/menu/all
    │
    ▼
SysMenuController → SysPermissionService
    ├── 查询当前用户所有权限
    ├── 过滤 perm_type = M 或 C
    ├── 过滤 status = 正常
    └── 构建树形结构（parentId 递归）
    │
    ▼
返回 MenuVO 树（path、component、icon、children）
```

### 6.2 按钮权限

前端通过以下方式获取按钮权限：

1. 登录时 `LoginUserVO.permissions` 携带权限码列表
2. 或调用 `GET /sysPermission/listAllPermCodes`（超管场景）
3. 页面内 `v-if` / 自定义指令判断 `perm_code` 是否包含

### 6.3 角色权限分配

```
GET /sysRole/permission/tree          → 完整权限树
GET /sysRole/{roleId}/permissionIds   → 角色已有权限 ID
POST /sysRole/{roleId}/permission     → 保存分配（先删后插）
    │
    ▼
清除该角色下所有用户的权限缓存 user:perm:{userId}
```

---

## 7. 权限码清单

### 7.1 系统管理 `system:*`

| 权限码 | 说明 |
|--------|------|
| `system:user:list` | 用户列表 |
| `system:user:edit` | 用户编辑 |
| `system:user:changeStatus` | 用户状态 |
| `system:user:resetPwd` | 重置密码 |
| `system:user:delete` | 删除用户 |
| `system:user:batchDelete` | 批量删除 |
| `system:user:assign` | 分配角色 |
| `system:role:list` | 角色列表 |
| `system:role:add` | 新增角色 |
| `system:role:edit` | 编辑角色 |
| `system:role:delete` | 删除角色 |
| `system:role:batchDelete` | 批量删除角色 |
| `system:role:changeStatus` | 角色状态 |
| `system:role:assign` | 分配权限 |
| `system:permission:list` | 权限列表 |
| `system:permission:add` | 新增权限 |
| `system:permission:edit` | 编辑权限 |
| `system:permission:delete` | 删除权限 |
| `system:permission:changeStatus` | 权限状态 |
| `system:menu:list` | 菜单列表 |
| `system:menu:add` | 新增菜单 |
| `system:menu:edit` | 编辑菜单 |
| `system:menu:delete` | 删除菜单 |
| `system:menu:changeStatus` | 菜单状态 |
| `system:online:list` | 在线用户 |
| `system:online:forceLogout` | 强制下线 |
| `system:online:redis:list` | Redis Key 列表 |
| `system:online:redis:delete` | 删除 Redis Key |
| `system:loginlog:list` | 登录日志 |
| `system:loginlog:export` | 导出登录日志 |
| `system:loginlog:clear` | 清空登录日志 |
| `system:operlog:list` | 操作日志 |
| `system:operlog:export` | 导出操作日志 |

### 7.2 商品管理 `goods:*`

| 权限码 | 说明 |
|--------|------|
| `goods:brand:list/add/edit/delete/batchDelete/changeStatus` | 品牌管理 |
| `goods:category:list/add/edit/delete/batchDelete/changeStatus/batchStatus` | 分类管理 |
| `goods:product:list/add/edit/delete/batchDelete/changeShelf/batchShelf` | 商品管理 |

### 7.3 库存管理 `inventory:*`

| 权限码 | 说明 |
|--------|------|
| `inventory:stock:list/edit` | 库存列表 |
| `inventory:stockin:list/add` | 入库管理 |
| `inventory:stockout:list/add` | 出库管理 |
| `inventory:warn:list/edit` | 库存预警 |
| `inventory:flow:list/export` | 库存流水 |

### 7.4 订单管理 `order:*`

| 权限码 | 说明 |
|--------|------|
| `order:info:list/add/pay/receive/cancel` | 订单管理 |
| `order:delivery:list/delivery` | 发货管理 |
| `order:refund:list/apply/approve/reject` | 退款管理 |

### 7.5 系统监控 `monitor:*`

| 权限码 | 说明 |
|--------|------|
| `monitor:api:list` | API 监控 |
| `monitor:server:view` | 服务监控 |
| `monitor:redis:view` | Redis 监控 |

---

## 8. 安全策略

| 策略 | 实现 |
|------|------|
| 密码存储 | BCrypt 单向加密 |
| 会话管理 | 无状态 JWT + Redis 登录态双重校验 |
| CSRF | 关闭（前后端分离 + Token 鉴权） |
| CORS | `WebConfig` 配置允许源 |
| 接口限流 | `@RateLimit`（注册等敏感接口） |
| 强制下线 | 删除 Redis Token，下次请求 1102 |
| 权限实时生效 | 角色/权限变更时清除 `user:perm:{userId}` 缓存 |
| 操作审计 | `@OperationLog` 记录增删改操作 |

---

## 9. 前后端协作约定

| 场景 | 前端行为 |
|------|----------|
| 登录成功 | 存储 accessToken / refreshToken |
| 请求接口 | 请求头携带 `Authorization: Bearer {accessToken}` |
| 收到 code=1102 | 自动调用 `/auth/refreshToken` 刷新 |
| 收到 code=1201 | 提示无权限，跳转 403 页面 |
| 侧边栏渲染 | 调用 `/system/menu/all` |
| 按钮显隐 | 判断 permissions 数组是否包含 perm_code |
| 登出 | 调用 `/auth/logout` 并清除本地 Token |
