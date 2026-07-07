# 工业视觉检测结果复核与质量闭环管理系统

## 项目定位

本项目是一个面向工业视觉质检场景的 Java 后端项目原型，用于模拟视觉检测结果进入企业质量管理流程后的复核、不合格记录和整改闭环。

它关注的不是模型训练本身，而是模型检测结果进入企业业务系统之后，如何被人工确认、追溯到生产批次，并进一步形成 NCR 与 CAPA 的质量闭环。

## 项目背景

YOLO 等视觉检测模型可以输出缺陷类别、置信度和检测框坐标，但在真实质量管理场景中，模型结果通常不能直接作为最终质量结论。

企业质量流程还需要：

- 人工复核模型检测结果，区分真实缺陷、误检和需复检。
- 将确认缺陷追溯到生产批次、检测任务和原始图片。
- 对确认缺陷生成 NCR 不合格记录。
- 对需要整改的问题创建 CAPA，并跟踪关闭结果。
- 通过状态流转保证批次质量状态可解释、可追溯。

## 核心业务流程

```text
生产批次
→ 检测任务
→ YOLO JSON 导入
→ 检测结果
→ 人工复核
→ NCR 不合格记录
→ CAPA 整改闭环
```

## 技术栈

后端：

- Java 21
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Knife4j / Swagger
- Maven
- Lombok

前端：

- Vue 3
- Vite
- Element Plus
- Vue Router
- Axios

## 已完成功能模块

- 批次管理：创建、分页查询、详情、基础信息更新、状态更新。
- 检测任务管理：创建任务、分页查询、详情、按批次查询、状态更新。
- YOLO JSON 导入：解析检测 JSON，写入图片与检测结果。
- 检测结果查询：按任务、图片、类别、复核状态查询检测结果。
- 人工复核：对检测结果确认缺陷、标记误检或要求复检。
- NCR：基于确认缺陷的复核记录创建不合格记录，并更新批次状态。
- CAPA：基于 OPEN NCR 创建整改闭环，支持更新、验证、关闭和同步关闭批次。
- 前端页面：Dashboard、批次、检测任务、检测结果、人工复核、NCR、CAPA 主链路页面。

## 快速启动

### 后端启动

进入后端目录，并通过环境变量传入本地 MySQL 密码：

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system\backend
set MYSQL_PASSWORD=your_mysql_password
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

说明：真实项目中数据库密码不应写入配置文件或提交到代码仓库。本项目的 `application.yml` 使用 `MYSQL_USERNAME` / `MYSQL_PASSWORD` 环境变量读取数据库账号密码。

### 前端启动

进入前端目录，安装依赖并启动 Vite：

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system\frontend
npm.cmd install
npm.cmd run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

前端通过 Vite 代理访问后端：

```text
/api -> http://localhost:8081
```

## 访问地址

后端接口文档：

```text
http://localhost:8081/doc.html
```

前端页面：

| 页面 | 地址 | 说明 |
| --- | --- | --- |
| Dashboard | `http://localhost:5173/` | 展示主流程概览 |
| 批次管理 | `http://localhost:5173/batches` | 查询批次与质量状态 |
| 批次详情 | `http://localhost:5173/batches/{id}` | 查看批次详情和该批次下检测任务 |
| 检测任务 | `http://localhost:5173/inspection-tasks` | 查询检测任务 |
| 检测结果 | `http://localhost:5173/detections` | 查询检测框和复核状态 |
| 人工复核 | `http://localhost:5173/reviews` | 查看复核记录，可从检测结果页提交复核 |
| NCR | `http://localhost:5173/ncrs` | 查询 NCR，可创建 CAPA |
| CAPA | `http://localhost:5173/capas` | 查询、编辑和关闭 CAPA |

## 数据库初始化

数据库建议使用 MySQL 8。初始化文件位于 `sample-data` 目录：

- `sample-data/init_schema.sql`：创建 MVP 核心表结构。
- `sample-data/sample_seed_data.sql`：写入脱敏演示数据。
- `sample-data/sample_yolo_detection.json`：YOLO JSON 导入接口的示例数据。

推荐执行顺序：

```sql
CREATE DATABASE IF NOT EXISTS visual_qms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE visual_qms;
SOURCE sample-data/init_schema.sql;
SOURCE sample-data/sample_seed_data.sql;
```

如果使用 MySQL Workbench，也可以手动打开 SQL 文件后依次执行。

## 项目截图

截图目录：

```text
screenshots/
```

建议保留以下关键页面截图，便于作品集展示和面试讲解：

- `dashboard.png`：系统首页与主流程。
- `batch-list.png`：批次列表与状态展示。
- `detection-results.png`：检测结果与人工复核入口。
- `review-list.png`：人工复核记录。
- `ncr-list.png`：NCR 不合格记录。
- `capa-list.png`：CAPA 整改闭环。

## 项目亮点

- 不是简单 CRUD，而是围绕工业视觉质检后的质量闭环设计。
- 有明确的业务状态流转，覆盖批次、检测任务、检测结果、NCR 和 CAPA。
- 有 YOLO JSON 到业务数据表的转换逻辑。
- 将模型检测结果与人工复核结论分离，便于追溯和扩展。
- 引入 NCR / CAPA 质量流程，贴近企业质量管理语境。
- 使用事务保证创建 NCR、创建 CAPA、关闭 CAPA 等跨表状态一致性。

## 非目标说明

本项目不是完整 MES / QMS 系统，不包含真实企业数据，不做在线模型推理，不接入真实工业相机或产线设备，也不面向工业现场部署。

它是一个用于学习、作品集展示和面试讲解的后端原型项目。
