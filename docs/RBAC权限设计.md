# RBAC 权限设计

> [← 返回 README](../README.md)

用户 → 角色 → 权限，登录后发 Token，接口用权限码校验。

![RBAC 权限说明](images/rbac.png)

---

## 1. 数据库关系（五张表）

| 表 | 作用 |
|----|------|
| `sys_user` | 用户账号 |
| `sys_role` | 角色（如管理员、店员） |
| `sys_permission` | 权限 / 菜单 / 按钮 |
| `sys_user_role` | 用户 ↔ 角色（多对多） |
| `sys_role_permission` | 角色 ↔ 权限（多对多） |

**用户最终权限 = 所有角色权限的并集**

---

## 2. 权限三级

| 类型 | 代码 | 前端表现 |
|------|------|----------|
| 目录 | M | 侧边栏一级菜单 |
| 菜单 | C | 具体页面 |
| 按钮 | F | 页面里的增删改按钮 |

权限码格式：`模块:资源:操作`，例如 `goods:product:list`

---

## 3. 登录 & 访问流程

1. **登录** → 校验账号密码 → 签发 AccessToken + RefreshToken
2. **调接口** → 请求头带 `Authorization: Bearer {token}`
3. **后端校验** → JWT 合法 + Redis 里有登录态 → `@RequiresPerm` 查权限码
4. **没权限** → 返回 `1201`；**Token 过期** → 返回 `1102`，前端用 RefreshToken 刷新

---

## 4. 前端怎么用

- **侧边栏菜单**：`GET /system/menu/all`，只返回用户有权限的 M、C
- **按钮显隐**：看登录返回的 `permissions` 数组里有没有对应权限码
- **SUPER_ADMIN**：超级管理员跳过权限校验，拥有全部能力

---

## 5. 管理后台怎么配

1. **角色管理** → 勾选权限树，保存到 `sys_role_permission`
2. **用户管理** → 给用户分配角色，保存到 `sys_user_role`
3. 改完角色/权限后会清 Redis 缓存，下次请求生效

---

## 相关文档

- [API 接口设计](API接口设计.md) — 认证、系统管理接口
- [数据库设计](数据库设计.md) — RBAC 表结构
