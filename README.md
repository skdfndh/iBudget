# iBudget 记账应用 💰

一个面向个人与家庭的记账应用，包含 Web 前端与桌面端。支持用户认证、账目与预算管理、离线优先同步、统计与图表展示。

## 🧩 项目概述

- 架构：Spring Boot + JPA/SQLite + 原生 HTML/JS + JavaFX
- 模块：认证、交易、预算、同步、统计、桌面客户端
- 目标：简单、可靠、可离线工作，支持多设备同步

## ✨ 功能列表

- 👤 用户与认证
  - 注册（强密码校验与二次确认）
  - 登录/登出（JWT `accessToken`/`refreshToken`）
  - 恢复密钥重置密码（注册时自动生成 8 位）
  - 多设备会话管理（最多 5 台）
- 📒 账目管理
  - 新增/编辑/删除交易，筛选与列表
- 🎯 预算管理
  - 月度预算与分类预算，超额提醒
- 🔄 数据同步
  - 增量拉取与推送，LWW 冲突解决
- 📊 统计与图表
  - 趋势、分类占比、收支曲线等
- 🖥️ 桌面端
  - JavaFX UI，基础账目与同步

## 🛠️ 技术栈

- 后端：Spring Boot 3.1.5、Spring Security、JPA/Hibernate、SQLite
- 前端：HTML5、CSS3、原生 JavaScript
- 桌面：JavaFX 21
- 构建：Maven

## 🗂️ 目录结构

```
accounting-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/accounting/
│   │   │   ├── AccountingApplication.java     # 应用入口
│   │   │   ├── api/                           # REST 控制器（Web）
│   │   │   │   ├── AuthController.java        # 认证与令牌
│   │   │   │   ├── TransactionsController.java# 交易接口
│   │   │   │   ├── BudgetController.java      # 预算接口
│   │   │   │   ├── StatsController.java       # 统计接口
│   │   │   │   ├── SyncController.java        # 同步接口
│   │   │   │   └── TokensController.java      # 令牌辅助接口
│   │   │   ├── config/                        # 安全与配置
│   │   │   │   ├── SecurityConfig.java        # Spring Security 配置
│   │   │   │   └── JwtAuthenticationFilter.java# JWT 认证过滤器
│   │   │   ├── service/                       # 业务层（服务）
│   │   │   │   ├── TransactionService.java    # 交易服务
│   │   │   │   ├── BudgetService.java         # 预算服务
│   │   │   │   ├── StatisticService.java      # 统计服务
│   │   │   │   ├── SyncService.java           # 同步服务
│   │   │   │   └── UserService.java           # 用户/认证服务
│   │   │   ├── service/local/                 # 桌面端本地实现
│   │   │   │   ├── LocalTransactionService.java
│   │   │   │   ├── LocalBudgetService.java
│   │   │   │   ├── LocalStatisticService.java
│   │   │   │   └── LocalAIAnalysisService.java
│   │   │   ├── repository/                    # 仓储层（JPA）
│   │   │   │   ├── TransactionRepository.java
│   │   │   │   ├── BudgetRepository.java
│   │   │   │   ├── SyncLogRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── UserTokenRepository.java
│   │   │   ├── model/                         # 数据模型
│   │   │   │   ├── Transaction.java
│   │   │   │   ├── Budget.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── SyncLog.java
│   │   │   │   ├── User.java
│   │   │   │   └── UserToken.java
│   │   │   ├── ui/                            # 桌面端
│   │   │   │   ├── MainApplication.java       # JavaFX 主程序
│   │   │   │   ├── ApiClient.java             # 桌面端 HTTP 客户端
│   │   │   │   └── DateSelector.java/StyleUtil.java
│   │   │   ├── chart/                         # 图表组件（桌面端）
│   │   │   │   ├── ChartAnalyzer.java
│   │   │   │   ├── BarChartView.java
│   │   │   │   ├── LineChartView.java
│   │   │   │   └── PieChartView.java
│   │   │   ├── storage/                       # 桌面端存储
│   │   │   │   └── StorageManager.java
│   │   │   └── util/                          # 工具类
│   │   │       ├── JwtUtil.java               # JWT 生成与解析
│   │   │       └── LocalDateAdapters.java     # 时间序列工具
│   │   └── resources/
│   │       ├── application.properties         # 应用配置
│   │       └── static/                        # Web 页面与脚本
│   │           ├── index.html                 # 入口页/导航
│   │           ├── login.html                 # 登录
│   │           ├── register.html              # 注册
│   │           ├── transactions.html          # 交易管理
│   │           ├── budgets.html               # 预算管理
│   │           ├── charts.html                # 图表展示
│   │           ├── trends.html                # 趋势分析
│   │           └── dashboard.html             # 总览面板
└── README.md
```

## 🚀 快速开始

- 前置环境
  - `JDK 17+`、`Maven 3.6+`、现代浏览器
- 启动 Web 端
  - `cd accounting-app`
  - `mvn clean compile`
  - `mvn spring-boot:run`
  - 访问 `http://localhost:8080`（登录页 `login.html`，注册页 `register.html`）
- 启动桌面端（Windows）
  - `run-desktop.bat`

## ⚙️ 配置说明

- `src/main/resources/application.properties`
  - `server.port=8080`
  - `spring.datasource.url=jdbc:sqlite:accounting.db`
  - `spring.jpa.hibernate.ddl-auto=update`
  - `logging.level.com.accounting=DEBUG`
  - `jwt.secret=${JWT_SECRET:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}`
  - 建议在系统环境中设置 `JWT_SECRET`

## 🧭 页面与交互

- `login.html` 登录：用户名/密码，登录成功保存 `token` 到 `localStorage`
- `register.html` 注册：用户名、密码与确认；成功后弹窗展示恢复密钥
- 其他：`transactions.html`、`budgets.html`、`trends.html`、`charts.html`、`dashboard.html`

## 🔌 API 概览

- 认证
  - `POST /api/auth/register` 注册（`username`, `password`, `confirmPassword`）→ 返回 `recoveryKey`
  - `POST /api/auth/login` 登录 → 返回 `accessToken`/`refreshToken`（兼容字段 `token`）
  - `POST /api/auth/reset-password` 重置密码（`username`, `recoveryKey`, `newPassword`）
  - `POST /api/auth/refresh` 刷新令牌（旋转 `refreshToken` 并续期）
  - `POST /api/auth/logout` 登出（移除 `refreshToken`）
- 认证约定
  - 请求头：`Authorization: Bearer <accessToken>`
  - 刷新：`refreshToken` 仅用于换取新的 `accessToken` 并自动旋转
- 同步
  - `GET /api/sync?last_version=<n>` 增量拉取
  - `POST /api/sync` 推送并合并（LWW）
  - `GET /api/sync/transactions` 当前用户交易列表
  - `POST /api/sync/transactions/upload` 上传交易并返回列表
- 交易与预算（示例）
  - `GET/POST/PUT /api/transactions`
  - `GET/POST/PUT /api/budgets`
  - `GET /api/stats/...` 统计数据

## 🔄 同步机制

- 增量同步：客户端携带 `lastVersion`，服务端返回变更与当前版本
- 冲突解决：按 `updatedAt` 进行 LWW（最后写入胜出）
- 变更日志：持久化 `SyncLog`，供其他设备拉取

## 🔐 安全设计

- BCrypt 密码哈希存储
- JWT 无状态鉴权（HMAC-SHA256）
- 恢复密钥重置密码（自动轮换）
- 多设备治理（最多 5 个刷新令牌，超限清理旧会话）
- 输入校验与错误响应：Controller 层统一校验与标准错误码

## 🧑‍💻 开发说明

- 分层职责：Controller（HTTP）、Service（业务）、Repository（数据）、Model（实体）
- 编译/运行
  - `mvn clean compile`
  - `mvn spring-boot:run`
- 桌面端
  - `run-desktop.bat`
- 环境变量
  - `JWT_SECRET` 用于签发 JWT，生产环境务必设置

## 📄 许可证

本项目用于学习与个人使用。

