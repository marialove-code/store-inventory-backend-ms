# 面试题：RBAC 与 Redis（基于本项目）

> [← 返回 README](../README.md)  
> 本文档根据 `store-inventory-backend` **真实代码**整理，可直接用于面试准备或项目讲解。

---

## 一、项目里 RBAC 和 Redis 各干什么（先建立整体印象）

### RBAC 负责什么

**解决「谁能访问哪个接口、看到哪些菜单和按钮」。**

- 数据库：用户 → 角色 → 权限（五张表，多对多）
- 登录：查角色和权限码，放进 `LoginUserVO` 返回前端
- 鉴权：`JwtAuthenticationFilter` 先确认「已登录」，`@RequiresPerm` + `PermissionAspect` 再确认「有权限」
- 菜单：`GET /system/menu/all`，超管看全部，普通用户只看自己有权限的 M/C 菜单

### Redis 负责什么

**解决「登录态在线管理、限流、监控」，不是业务主数据存储。**

| 用途 | Key 模式（实际带前缀 `inventory:`） | 说明 |
|------|-------------------------------------|------|
| AccessToken 登录态 | `user:token:{userId}:access:{token}` | 存 `LoginUserVO`，JWT 合法还要 Redis 有记录 |
| RefreshToken | `user:token:{userId}:refresh:{token}` | 刷新/校验刷新令牌 |
| 用户权限缓存 | `user:perm:{userId}` | 登录时写入；权限变更时删除 |
| 多设备映射 | `user:device:{userId}` | Hash：`accessToken → refreshToken` |
| 接口限流 | `limit:{uri}:{ip}` | `@RateLimit` 计数 |
| Redis 监控 | `monitor:redis:trend` 等 | 运维监控模块 |

> **面试可强调：** 本项目是 **JWT + Redis 双校验**，不是「有 JWT 就一定有效」，被踢下线、登出后 Redis 里没有登录态，请求会被拦。

---

## 二、RBAC 实现要点（对照代码）

### 2.1 数据模型

```
sys_user ── sys_user_role ── sys_role ── sys_role_permission ── sys_permission
```

- 一个用户多个角色，权限取**并集**
- `sys_permission.perm_type`：**M** 目录、**C** 菜单、**F** 按钮
- 权限码格式：`模块:资源:操作`，如 `goods:product:list`
- 超管角色码：`SUPER_ADMIN`，权限码通配：`*:*:*`

### 2.2 认证 vs 授权（两层）

| 层级 | 组件 | 作用 |
|------|------|------|
| 认证（是否登录） | `SecurityConfig` + `JwtAuthenticationFilter` | 白名单放行；其它请求需合法 JWT + Redis 登录态 |
| 授权（能否操作） | `@RequiresPerm` + `PermissionAspect` | 比对用户权限码；`SUPER_ADMIN` 直接放行 |

Spring Security 里 `GrantedAuthority` 设为空列表，**细粒度权限不走 Spring Security 原生角色**，而是自定义 AOP。

### 2.3 一次请求的权限链路

```
请求带 Authorization: Bearer {accessToken}
    → JwtAuthenticationFilter：JWT 签名/过期 + Redis 取 LoginUserVO
    → LoginUserContext（ThreadLocal）写入当前用户
    → Spring Security：authenticated
    → PermissionAspect（仅 @RequiresPerm 方法）：查权限码
    → Controller 业务
    → finally：LoginUserContext.clear() 防线程复用串号
```

### 2.4 前端如何配合

- 登录响应带 `roles`、`permissions`
- 侧边栏：`/system/menu/all` 动态菜单树
- 按钮：前端用 `permissions` 数组做显隐（后端接口仍要 `@RequiresPerm` 兜底）

---

## 三、Redis 实现要点（对照代码）

### 3.1 为什么 JWT 之外还要 Redis

| 仅 JWT | JWT + Redis（本项目） |
|--------|----------------------|
| 无法服务端主动失效 Token | 登出、踢人、改权限可删 Redis Key |
| 难做多设备管理 | `user:device` Hash 管理多终端 |
| 难查在线用户 | 扫描 `user:token:*:access:*` |

### 3.2 登录时写了哪些 Redis

`AuthServiceImpl.login()` 大致四步：

1. `user:token:{userId}:access:{accessToken}` → 存 `LoginUserVO`，TTL = access 过期（120 分钟）
2. `user:token:{userId}:refresh:{refreshToken}` → 存 userId，TTL = 7 天
3. `user:perm:{userId}` → 存权限码列表
4. `user:device:{userId}` → Hash 记录 access ↔ refresh 映射

### 3.3 登出与强制下线

- **单设备登出**：`UserSessionServiceImpl.logoutByToken()` 删 access、refresh、device 映射
- **踢全部设备**：`kickUserOffline()` 遍历 device Hash，逐个登出，并删 `user:perm`
- **触发踢人场景**：用户禁用/删除、角色权限变更、权限树变更（`SysRoleServiceImpl`、`SysPermissionServiceImpl`、`SysUserServiceImpl`）

### 3.4 接口限流

`RateLimitAspect`：`limit:{uri}:{ip}`，`INCR` + 首次设置过期时间，超过阈值直接返回 JSON，不进入业务。

### 3.5 诚实说明（面试加分）

- 登录时写了 `user:perm` 缓存，但 `PermissionAspect` 里权限校验目前走 **`listPermCodesByUserId` 查库**，未优先读 Redis——可答「已预留缓存 Key，后续可改为 Cache-Aside」
- 在线用户列表用 `redisTemplate.keys()` 扫描，数据量大时有性能风险——可答「生产可改 SCAN 或维护在线 Set」

---

## 四、RBAC 面试题与参考答案

### Q1：你们项目 RBAC 是怎么设计的？

**答：** 经典五表模型：用户、角色、权限，中间两张关联表。权限分目录、菜单、按钮三级，接口用 `perm_code` 标识，Controller 方法上标 `@RequiresPerm("goods:product:list")`，由 AOP 切面校验。用户最终权限是所有角色权限的并集。超级管理员角色 `SUPER_ADMIN` 在切面里直接放行，并返回全部菜单。

---

### Q2：认证和授权有什么区别？你们分别怎么做的？

**答：**

- **认证**：证明「你是谁」——登录校验账号密码（BCrypt），签发 JWT，Redis 存登录态。
- **授权**：证明「你能干什么」——`@RequiresPerm` 校验权限码。

Filter 管登录，AOP 管权限，职责分离，比把所有权限塞进 Spring Security 的 `GrantedAuthority` 更清晰。

---

### Q3：为什么菜单和按钮要分成 M / C / F？

**答：**

- **M（目录）**：侧边栏一级分组
- **C（菜单）**：具体页面路由
- **F（按钮）**：页面内操作，如新增、删除

菜单接口只返回 M+C 构树；F 不渲染路由，只参与权限码校验和前端按钮显隐。这样路由结构和操作粒度分开，扩展方便。

---

### Q4：`@RequiresPerm` 和 Spring Security 的 `@PreAuthorize` 有什么区别？为什么用自定义注解？

**答：** 本项目 `@PreAuthorize` 未接权限码体系，而是用自定义 `@RequiresPerm` + `PermissionAspect`，直接对接 `sys_permission.perm_code`，和前端按钮、菜单同一套编码。好处是与业务表结构一致；代价是自己维护切面，要处理好超管放行、未登录、无权限等异常。

---

### Q5：超级管理员怎么实现的？能删吗？

**答：** 角色编码 `SUPER_ADMIN`。`PermissionAspect` 里若 `roles` 包含该编码直接 return。`SysMenuController` 里超管查全部 M/C 菜单。`SysUserServiceImpl` 对用户名 `super_admin` 做删除保护。设计上超管角色和用户都不允许删，防止系统锁死。

---

### Q6：用户角色变更后，权限怎么生效？

**答：** 改 `sys_user_role` 或 `sys_role_permission` 后，调用 `userSessionService.kickUserOffline(userId)`，清掉该用户所有 Token 和 `user:perm` 缓存。用户下次请求 Redis 无登录态，需重新登录，重新加载权限。这是**强制失效**策略，保证安全，代价是用户体验上需要重新登录。

---

### Q7：如果只做接口权限，不做按钮权限可以吗？

**答：** 可以，但前端按钮可能误展示。正确做法是**前后端双重校验**：前端隐藏无权限按钮（体验），后端 `@RequiresPerm` 拦截（安全）。安全边界必须在后端。

---

### Q8：RBAC 和 ABAC 有什么区别？为什么选 RBAC？

**答：** RBAC 按角色授权，模型简单，适合后台管理系统。ABAC 按属性（部门、时间、IP 等）动态判断，更灵活但更复杂。门店进销存后台用户角色相对固定（管理员、店员），RBAC 够用且易维护。

---

## 五、Redis 面试题与参考答案

### Q1：项目里 Redis 主要用在哪些场景？

**答：** 四类：

1. **登录态**：Access/Refresh Token 及 `LoginUserVO`
2. **会话治理**：多设备映射、踢人下线、在线用户
3. **权限缓存**：`user:perm`（写入与失效已有，读可优化）
4. **接口限流**：`@RateLimit` 基于 INCR 计数  
   另有监控模块存 Redis 趋势、大 Key 等运维数据。

---

### Q2：已经有 JWT 了，为什么还要 Redis 存 Token？

**答：** JWT 无状态，服务端签发后无法主动作废（在不过黑名单的前提下）。Redis 存登录态后：

- 登出可删 Key，立即失效
- 管理员可强制下线
- 改权限后可踢人，避免旧 Token 继续访问

这是 **有状态会话 + 无状态 JWT** 的折中：JWT 携带身份，Redis 控制「是否仍允许使用」。

---

### Q3：AccessToken 和 RefreshToken 分别多长？为什么分开？

**答：** 配置在 `application.yml`：Access **120 分钟**，Refresh **7 天**（10080 分钟）。Access 短降低泄露风险；Refresh 长用于无感续期。Refresh 接口会校验 Redis 里是否存在对应 Key，防止已注销的 Refresh 继续使用。

---

### Q4：描述一下登录到访问接口的完整流程

**答：**

1. `POST /auth/login`，BCrypt 校验密码
2. 查角色、权限码；生成 AccessToken、RefreshToken（HS256）
3. Redis 写入 access、refresh、perm、device 四类 Key
4. 后续请求 Header 带 `Bearer {accessToken}`
5. `JwtAuthenticationFilter`：验 JWT → 用 `userId` 和 token 拼 Redis Key 取 `LoginUserVO`
6. 有则设置 `LoginUserContext`，放行
7. 带 `@RequiresPerm` 的接口再走权限切面

---

### Q5：Redis Key 怎么设计的？为什么把 token 放在 Key 里？

**答：** 格式 `user:token:{userId}:access:{accessToken}`。带上 **userId** 便于按用户清理；带上 **token 本身** 支持同一用户多设备多 Token 并存。全局还有 `inventory:` 前缀（`spring.data.redis.key-prefix`），避免多项目共 Redis 时冲突。

---

### Q6：如何实现「踢其他设备下线」？

**答：** `user:device:{userId}` 是 Hash，field 为 accessToken，value 为 refreshToken。`kickOtherDevices()` 遍历 Hash，保留当前请求的 accessToken，其余调用 `logoutByToken()` 删除对应 access、refresh 及映射。

---

### Q7：接口限流怎么实现的？是滑动窗口吗？

**答：** 基于 Redis `INCR` + `EXPIRE`：Key 为 `limit:{uri}:{ip}`，第一次访问设过期时间，窗口内计数超过 `@RateLimit` 配置则拒绝。这是**固定窗口计数**，实现简单；严格滑动窗口可用 ZSET 按时间戳打分，或 Redisson `RRateLimiter`。

---

### Q8：Redis 和数据库一致性怎么保证？

**答：** 本项目业务数据以 **PostgreSQL 为准**，Redis 是缓存和会话，不做库存等核心数据的唯一来源。登录态场景是 **Cache Aside**：登录写 Redis，失效删 Key，不以 Redis 为准写回库。权限变更通过 **踢下线** 保证最终一致，而不是实时双写。

---

### Q9：Redis 挂了会怎样？有没有降级？

**答：** 当前实现强依赖 Redis：无登录态缓存则 Filter 判失效，用户需重新登录；限流、在线用户也会受影响。面试可答：生产可加 Redis 哨兵/集群；核心读路径可做降级策略（如仅 JWT 校验紧急放行），但本项目未实现，属于可改进点。

---

### Q10：`keys *` 扫描在线用户有什么问题？

**答：** `OnlineUserServiceImpl` 使用 `keys` 匹配 `user:token:*:access:*`。`KEYS` 会阻塞 Redis，Key 多时影响性能。生产环境应改用 `SCAN` 迭代，或登录时维护 `online_users` Set，登出时移除。

---

## 六、综合场景题（高频）

### Q1：用户反馈「明明有权限但接口 1201」，你怎么排查？

**答：**

1. 看登录返回的 `permissions` 是否含该 `perm_code`
2. 看接口 `@RequiresPerm` 值是否与库中 `sys_permission.perm_code` 一致
3. 看是否角色未分配、权限被禁用、逻辑删除
4. 看是否改权限后未重新登录（旧 `LoginUserVO` 在 Redis 里权限可能过期）
5. 超管应用 `SUPER_ADMIN` 角色，不是只看用户名

---

### Q2：Token 没过期但被踢下线，原因有哪些？

**答：**

- 主动登出，Redis Key 已删
- 管理员强制下线
- 用户被禁用/删除
- 角色或权限被修改，触发 `kickUserOffline`
- Redis Key 过期（TTL 与 JWT 过期时间需结合看）

JWT 未过期但 Redis 无记录，Filter 返回 1102，这是**预期行为**。

---

### Q3：如何设计权限变更「不踢人也能实时生效」？

**答（可答优化方案）：**

- 方案 A：继续踢人（本项目）——实现简单，最安全
- 方案 B：权限校验读 Redis `user:perm`，变更时只删缓存，下次请求回源 DB 重建——不踢人但需保证切面读缓存
- 方案 C：网关层统一鉴权 + 版本号，权限变更递增 `permVersion`，Token 里带版本，不匹配则拒绝

---

### Q4：面试时如何用 2 分钟介绍这个项目的安全设计？

**答模板：**

> 我们是一套前后端分离的门店进销存后台。安全上分三层：Spring Security 无状态 + 自定义 JWT 过滤器做登录认证；Redis 存 Token 和登录用户信息，支持登出、踢人、多设备；RBAC 五表模型，自定义 `@RequiresPerm` 做接口级授权，菜单按钮三级权限码前后端统一。密码 BCrypt，敏感操作有操作日志，注册接口还有 Redis 限流防刷。

---

## 七、可主动说的亮点与改进点（显得真实）

### 亮点

- JWT 双 Token + Redis 双校验，能主动失效会话
- RBAC 到按钮级，前后端权限码统一
- 权限/角色变更联动踢下线
- 多设备 Hash 映射，支持踢其他设备
- `@RateLimit` 注解式限流，易扩展

### 可改进（诚实说反而加分）

- `user:perm` 写了但切面仍查库，可统一走缓存
- 在线用户 `keys` 扫描可改 SCAN
- 限流为固定窗口，高并发可用 Redisson 或 Lua 滑动窗口
- RefreshToken 刷新逻辑里权限未区分超管全量加载（可对照 `login` 补齐）

---

## 八、相关代码位置速查

| 模块 | 路径 |
|------|------|
| 登录 / Token / Redis 写入 | `modules/auth/service/impl/AuthServiceImpl.java` |
| JWT 过滤器 | `framework/security/filter/JwtAuthenticationFilter.java` |
| 权限切面 | `framework/security/permission/aspect/PermissionAspect.java` |
| 权限注解 | `framework/security/permission/annotation/RequiresPerm.java` |
| 会话踢人 | `modules/auth/service/impl/UserSessionServiceImpl.java` |
| Redis 常量 | `common/constants/RedisConstants.java` |
| 限流切面 | `framework/web/ratelimit/aspect/RateLimitAspect.java` |
| Security 配置 | `config/security/SecurityConfig.java` |
| 动态菜单 | `modules/system/permission/controller/SysMenuController.java` |

---

## 九、延伸阅读（本项目文档）

- [RBAC权限设计.md](RBAC权限设计.md)
- [系统架构设计.md](系统架构设计.md)
- [API接口设计.md](API接口设计.md)
