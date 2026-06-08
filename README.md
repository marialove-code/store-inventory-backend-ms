# 门店简易记账 · 后端

> 最初给自家门店用，帮母亲替代手工记账。  
> 从「门店销售 + 总览」起步，开发过程中逐步完善，现为 **V1.0**。  
> 开源到 Gitee，主要是**技术交流、学习展示**，自用项目，非商业产品。

[![Version](https://img.shields.io/badge/Version-V1.0-blue.svg)](#)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0+-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-0099FF?logo=mybatis&logoColor=white)](https://baomidou.com/)
[![Knife4j](https://img.shields.io/badge/Knife4j-4.3.0-85EA2D?logo=swagger&logoColor=black)](https://doc.xiaominfo.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## 这个项目是干什么的

母亲以前在门店靠本子手工记账——卖了什么、收了多少钱、还剩多少货，都要自己算。  
我试着写了一个小系统：**选商品、开一单、自动扣库存、记下金额和利润**，再在页面上看今天/本月卖了多少。

用着用着，从开发者角度又想把它完善一点，于是陆续加上了商品管理、库存入出库、订单、权限、监控等模块，就成了今天的 V1.0。

**适合谁看：** 想参考 Spring Boot 做小型门店 / 记账 / 进销存项目的开发者。

---

## 能做什么

### 最初的核心（门店记账）

| 能力 | 说明 |
|------|------|
| 门店开单 | 选商品、填数量，自动扣库存、记流水 |
| 销售流水 | 查看历史销售记录 |
| 营收统计 | 今日 / 本月销售额汇总 |
| 门店看板 | 营收概览、热销商品等 |

### 后来慢慢补上的

商品与分类、库存入出库与预警、线上订单与发货退款、用户角色权限、操作日志、数据看板、系统监控等——都是用着用着觉得「要是也有就好了」才加进去的，不是一开始就这么设计的。

---

## 功能截图

<!-- 建议补充：门店开单页、门店看板页，放入 docs/images/ 后取消注释 -->

<!-- ![门店开单](docs/images/shop-sale.png) -->
<!-- ![门店看板](docs/images/shop-dashboard.png) -->

> 截图待补充，可参考 [docs/images/README.md](docs/images/README.md)

---

## 系统架构

![系统架构图](docs/images/architecture.png)

前后端分离的单体项目，门店记账和后台管理共用同一套后端，没有拆微服务。

> 详细架构见 [docs/系统架构设计.md](docs/系统架构设计.md)

---

## RBAC 权限说明

![RBAC 权限说明](docs/images/rbac.png)

- 登录后签发 Token，访问接口时在 Header 携带 `Authorization: Bearer {token}`
- 接口通过 `@RequiresPerm` 注解校验权限码，例如 `goods:product:list`
- 权限分三级：**目录（M）**、**菜单（C）**、**按钮（F）**，前端菜单和按钮据此控制显隐
- `SUPER_ADMIN` 角色拥有全部权限，管理后台配置用户与角色

> 详细设计见 [docs/RBAC权限设计.md](docs/RBAC权限设计.md)

---

## 业务流转

![业务流转图](docs/images/business-flow.png)

门店开单（项目初心）：选商品 → 校验库存 → 扣减库存 → 写入销售流水 → 看板统计今日/本月营收。  
对应接口：`POST /shop/sale` · `GET /shop/sale/stats` · `GET /shop/record/list`

> 完整流程见 [docs/业务流转图.md](docs/业务流转图.md)

---

## 说明

这是**自家门店自用**的项目，功能按实际需求迭代，代码和文档都在持续完善中。  
如果对你有参考价值，欢迎 **Star**；有问题或建议，欢迎提 **Issue** 交流。

---

## 许可证

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
