我直接**帮你把这份后端 README 优化、精简、排版更清爽、更适合 Gitee 展示**，**和你前端 README 风格完全统一**，你复制粘贴就能用！

# 智联数贸平台 · 后端
> 门店 / 配件供应链管理系统 RESTful 后端服务  
> Spring Boot 3 分层架构 | 认证鉴权 | RBAC 权限 | 统一 API 规范

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17-blue.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-336791.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0%2B-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)

---

## 项目简介
**智联数贸平台后端**（`store-inventory-backend`）是面向门店与配件场景的**进销存 + 运营管理系统**，提供用户认证、权限控制、业务接口、系统监控等核心能力。

已实现：**认证中心、RBAC 权限体系、用户/角色/菜单管理、在线用户监控、操作审计**。
业务模块：**库存管理、订单管理、数据看板、AI 运营助手** 持续迭代中。

所有接口遵循统一 `Result` 响应规范，前后端协作高效、稳定。

---

## 技术栈
| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.2.5 |
| 开发语言 | JDK 17 |
| 安全认证 | Spring Security + JWT 双 Token |
| 缓存 | Redis 7+（Lettuce 连接池） |
| 数据库 | PostgreSQL 14+ |
| ORM | MyBatis-Plus 3.5.5 |
| 接口文档 | Knife4j（OpenAPI3） |
| 工具库 | Lombok、Hutool |

---

## 核心亮点
### ✅ JWT 双 Token 认证
- AccessToken（120分钟）：接口鉴权
- RefreshToken（7天）：无感刷新续期
- 请求头：`Authorization: Bearer token`

### ✅ Redis 会话控制
- 登录态存储 + 权限缓存
- 支持单点登录、强制下线
- 权限实时生效

### ✅ RBAC 权限体系
- 用户 → 角色 → 权限三级模型
- 接口级 + 按钮级权限控制
- 自定义注解 + AOP 实现权限校验

### ✅ 高可用工程能力
- 全局异常统一处理
- 操作日志自动审计
- 接口限流、防重复提交
- 跨域配置、安全防护

---

## 环境要求
- JDK 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 7.0+

---

## 快速启动
### 1. 克隆项目
```bash
git clone https://gitee.com/周士林/store-inventory-backend.git
cd store-inventory-backend
```

### 2. 创建数据库
```sql
CREATE DATABASE inventory_store;
```

### 3. 修改配置
修改 `application-dev.yml` 数据库、Redis 信息。

### 4. 运行项目
```bash
mvn clean package -DskipTests
java -jar target/store-inventory-backend.jar
```

或直接运行启动类：`com.inventory.InventoryApplication`

---

## 服务访问
- 接口文档：http://localhost:8080/api/doc.html
- 服务端口：8080
- 接口前缀：/api

---

## 目录结构
```
store-inventory-backend/
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── inventory/
        │
        │           ├── InventoryApplication.java
        │           │   └── SpringBoot 启动类
        │
        │           ├── common/                            # 公共基础层（全项目通用）
        │           │
        │           │   ├── base/                         # 基础父类
        │           │   │   ├── BaseController.java       # Controller公共方法
        │           │   │   └── BaseEntity.java           # 实体公共字段（createTime等）
        │           │
        │           │   ├── constants/                    # 全局常量
        │           │   │   ├── CommonConstants.java
        │           │   │   ├── PermissionConstants.java
        │           │   │   └── RedisConstants.java
        │           │
        │           │   ├── enums/                        # 全局枚举
        │           │   │   ├── DeletedFlagEnum.java
        │           │   │   └── OperationTypeEnum.java
        │           │
        │           │   ├── exception/                    # 公共异常
        │           │   │   └── BusinessException.java
        │           │
        │           │   ├── response/                     # 统一响应体
        │           │   │   ├── Result.java
        │           │   │   └── ResultCode.java
        │           │
        │           │   ├── utils/                        # 通用工具类
        │           │   │   ├── DateUtils.java
        │           │   │   ├── DesensitizeUtil.java
        │           │   │   ├── StringUtils.java
        │           │   │   └── ValidationUtils.java
        │           │
        │           │   └── validator/                    # 自定义参数校验器
        │           │
        │
        │           ├── config/                           # Spring 配置层
        │           │
        │           │   ├── jackson/                      # Jackson配置
        │           │   │   └── JacksonConfig.java
        │           │
        │           │   ├── mybatis/                      # MyBatisPlus配置
        │           │   │   ├── MybatisPlusInterceptorConfiguration.java
        │           │   │   └── MybatisPlusMetaObjectHandler.java
        │           │
        │           │   ├── redis/                        # Redis配置
        │           │   │   └── RedisConfig.java
        │           │
        │           │   ├── security/                     # SpringSecurity配置
        │           │   │   └── SecurityConfig.java
        │           │
        │           │   ├── swagger/                      # Knife4j/Swagger配置
        │           │   │   └── Knife4jConfiguration.java
        │           │
        │           │   └── web/                          # MVC/Web配置
        │           │       └── WebConfig.java
        │           │
        │
        │           ├── framework/                        # 基础设施层（核心工程化）
        │           │
        │           │   ├── log/                          # 日志体系
        │           │   │
        │           │   │   ├── annotation/               # 日志注解
        │           │   │   │   └── OperationLog.java
        │           │   │
        │           │   │   └── aspect/                   # 日志切面
        │           │   │       └── OperationLogAspect.java
        │           │
        │           │
        │           │   ├── redis/                        # Redis基础设施
        │           │   │   └── RedisUtils.java
        │           │
        │           │
        │           │   ├── security/                     # 安全体系
        │           │   │
        │           │   │   ├── context/                  # 登录用户上下文
        │           │   │   │   └── LoginUserContext.java
        │           │   │
        │           │   │   ├── exception/                # 权限异常
        │           │   │   │   └── PermissionException.java
        │           │   │
        │           │   │   ├── filter/                   # JWT过滤器
        │           │   │   │   └── JwtAuthenticationFilter.java
        │           │   │
        │           │   │   ├── jwt/                      # JWT工具
        │           │   │   │   └── JwtUtil.java
        │           │   │
        │           │   │   └── permission/               # 权限体系
        │           │   │       │
        │           │   │       ├── annotation/           # 权限注解
        │           │   │       │   └── RequiresPerm.java
        │           │   │       │
        │           │   │       └── aspect/               # 权限校验切面
        │           │   │           └── PermissionAspect.java
        │           │
        │           │
        │           │   └── web/                          # Web基础设施
        │           │       │
        │           │       ├── exception/                # 全局异常处理
        │           │       │   └── GlobalExceptionHandler.java
        │           │       │
        │           │       ├── interceptor/              # MVC拦截器
        │           │       │   └── LoginInterceptor.java
        │           │       │
        │           │       └── ratelimit/                # 接口限流
        │           │           │
        │           │           ├── annotation/
        │           │           │   └── RateLimit.java
        │           │           │
        │           │           └── aspect/
        │           │               └── RateLimitAspect.java
        │           │
        │
        │           ├── modules/                          # 业务模块层（领域层）
        │           │
        │           │   ├── auth/                         # 认证模块（登录注册）
        │           │   │
        │           │   │   ├── controller/
        │           │   │   ├── dto/
        │           │   │   ├── service/
        │           │   │   │   └── impl/
        │           │   │   └── vo/
        │           │
        │           │
        │           │   ├── goods/                        # 商品领域
        │           │   │
        │           │   │   ├── brand/                    # 商品品牌
        │           │   │   │   ├── controller/
        │           │   │   │   ├── convert/
        │           │   │   │   ├── dto/
        │           │   │   │   ├── entity/
        │           │   │   │   ├── mapper/
        │           │   │   │   ├── service/
        │           │   │   │   │   └── impl/
        │           │   │   │   └── vo/
        │           │   │
        │           │   │   ├── category/                 # 商品分类
        │           │   │   │   ├── controller/
        │           │   │   │   ├── convert/
        │           │   │   │   ├── dto/
        │           │   │   │   ├── entity/
        │           │   │   │   ├── mapper/
        │           │   │   │   ├── service/
        │           │   │   │   │   └── impl/
        │           │   │   │   └── vo/
        │           │   │
        │           │   │   └── product/                  # 商品SPU/SKU
        │           │   │       ├── controller/
        │           │   │       ├── convert/
        │           │   │       ├── dto/
        │           │   │       ├── entity/
        │           │   │       ├── mapper/
        │           │   │       ├── service/
        │           │   │       │   └── impl/
        │           │   │       └── vo/
        │           │
        │           │
        │           │   └── system/                       # 系统管理领域
        │           │
        │           │       ├── user/                     # 用户管理
        │           │       │   ├── controller/
        │           │       │   ├── convert/
        │           │       │   ├── dto/
        │           │       │   ├── entity/
        │           │       │   ├── mapper/
        │           │       │   ├── service/
        │           │       │   │   └── impl/
        │           │       │   └── vo/
        │           │
        │           │       ├── role/                     # 角色管理
        │           │       │   ├── controller/
        │           │       │   ├── entity/
        │           │       │   ├── mapper/
        │           │       │   ├── service/
        │           │       │   │   └── impl/
        │           │       │   └── vo/
        │           │
        │           │       ├── permission/               # 权限/菜单管理
        │           │       │   ├── controller/
        │           │       │   ├── entity/
        │           │       │   ├── mapper/
        │           │       │   ├── service/
        │           │       │   │   └── impl/
        │           │       │   └── vo/
        │           │
        │           │       ├── log/                      # 操作日志
        │           │       │   ├── controller/
        │           │       │   ├── entity/
        │           │       │   ├── mapper/
        │           │       │   ├── service/
        │           │       │   │   └── impl/
        │           │       │   └── vo/
        │           │
        │           │       ├── online/                   # 在线用户
        │           │       │   ├── controller/
        │           │       │   ├── entity/
        │           │       │   ├── service/
        │           │       │   │   └── impl/
        │           │       │   └── vo/
        │           │
        │           │       └── monitor/                  # 系统监控
        │           │           ├── controller/
        │           │           ├── service/
        │           │           └── vo/
        │           │
        │
        └── resources/
            │
            ├── application.yml                           # 主配置
            ├── application-dev.yml                       # 开发环境
            ├── application-uat.yml                       # 测试环境
            ├── application-prd.yml                       # 生产环境
            │
            └── mapper/                                   # MyBatis XML
                │
                ├── auth/
                │
                ├── goods/
                │   ├── brand/
                │   ├── category/
                │   └── product/
                │
                └── system/
                    ├── log/
                    ├── permission/
                    ├── role/
                    └── user/
```

---

## 核心模块
| 模块 | 说明 |
|------|------|
| 认证中心 | 登录、注册、刷新 Token |
| 用户管理 | 用户 CRUD、状态管理 |
| 角色管理 | 角色维护、权限绑定 |
| 权限管理 | RBAC 权限树 |
| 菜单管理 | 动态菜单配置 |
| 在线用户 | 会话监控、强制下线 |
| 操作日志 | 全链路审计 |
| 库存管理 | 规划中（库存、入库、出库、预警） |
| 订单管理 | 规划中（订单、状态机、发货） |
| 数据看板 | 规划中（统计、图表） |

---

## 前后端协作规范
### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 状态码约定
- 200：成功
- 401/1102：未登录 / Token 过期
- 403/1201：无权限

### 跨域
已支持前端 `localhost:5173` 本地调试。

---

## 配置说明
- 服务端口：8080
- 接口前缀：/api
- 数据库：PostgreSQL
- 缓存：Redis
- JWT 密钥、过期时间可配置

---

## 提交规范
- `feat` 新功能
- `fix` 修复bug
- `docs` 文档更新
- `refactor` 重构
- `chore` 构建/依赖

---

## 许可证
Apache License 2.0