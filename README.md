# 门店简易记账 · 后端

> 最初给自家门店用，帮母亲替代手工记账；从「门店销售 + 总览」起步，逐步完善为 V1.0。  
> 开源仅供**技术交流、学习展示**，自用项目。

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0+-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-0099FF?logo=mybatis&logoColor=white)](https://baomidou.com/)
[![Knife4j](https://img.shields.io/badge/Knife4j-4.3.0-85EA2D?logo=swagger&logoColor=black)](https://doc.xiaominfo.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## 能做什么

- **门店开单**：选商品、记一笔，自动扣库存、算金额和利润
- **销售统计**：查看流水，汇总今日 / 本月营收
- **商品 & 库存**：品牌分类、入出库、库存预警
- **订单管理**：下单锁库存、发货扣库存、退款回滚
- **权限 & 日志**：登录鉴权、角色权限、操作记录

---

## 系统架构

![系统架构图](docs/images/architecture.png)

---

## 业务流转

![业务流转图](docs/images/business-flow.png)

---

## RBAC 权限

![RBAC 权限说明](docs/images/rbac.png)

---

## 快速启动

**环境：** JDK 17 · Maven 3.8+ · PostgreSQL · Redis

```bash
git clone <你的仓库地址>
cd store-inventory-backend
```

1. 创建数据库 `inventory_store`
2. 修改 `src/main/resources/application.yml`，将 `spring.profiles.active` 改为 `dev`
3. 修改 `application-dev.yml` 中的数据库、Redis 连接（密码建议用环境变量 `DB_PWD`、`REDIS_PWD`）
4. 启动：

```bash
mvn spring-boot:run
```

**接口文档：** http://localhost:8080/api/doc.html

---

## 说明

自家门店自用项目，按实际需求迭代。欢迎 Star / Issue 交流。

**许可证：** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
