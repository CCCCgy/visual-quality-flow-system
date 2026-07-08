# 前后端演示脚本

本文用于面试、作品集录屏或现场演示。推荐先用前端页面完整展示质量闭环，再用 Knife4j 补充说明接口和状态流转。

## 1. 演示准备

### 方式一：使用本地 MySQL

如果本机已经安装 MySQL，可以按 `sample-data` 中的 SQL 初始化数据库，并在启动后端时把 `MYSQL_PASSWORD` 设置为本地 MySQL 密码。

初始化文件：

```text
sample-data/init_schema.sql
sample-data/sample_seed_data.sql
```

### 方式二：使用 Docker Compose 启动 MySQL

如果希望快速准备演示数据库，可以在项目根目录启动 Docker MySQL：

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system
docker compose up -d
docker ps
```

Docker MySQL 默认连接信息：

```text
host: localhost
port: 3306
database: visual_qms
username: root
password: visual_qms_password
```

如果需要重置 Docker 数据库并重新执行初始化 SQL：

```bat
docker compose down -v
docker compose up -d
```

说明：`docker compose down` 不会删除 named volume 中的数据，`docker compose down -v` 会删除数据库数据。

### 启动后端

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system\backend
set MYSQL_PASSWORD=your_mysql_password
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

如果使用 Docker MySQL，则使用：

```bat
set MYSQL_PASSWORD=visual_qms_password
```

后端接口文档：

```text
http://localhost:8081/doc.html
```

### 启动前端

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system\frontend
npm.cmd install
npm.cmd run dev
```

前端访问地址：

```text
http://localhost:5173
```

说明：如果演示数据中的编号重复，请替换日期或序号后重新执行。

## 2. 演示主线

```text
Dashboard 质量看板
-> 批次列表和批次详情
-> 检测任务
-> 检测结果列表
-> 检测结果详情与 bbox 可视化
-> 人工复核
-> 创建 NCR
-> 创建 CAPA
-> 关闭 CAPA 并核对状态同步
```

## 3. Step 1：打开 Dashboard，说明质量看板指标

页面路径：

```text
http://localhost:5173/
```

操作方式：

- 打开 Dashboard 首页。
- 说明顶部统计卡片：生产批次、检测任务、检测结果、待复核、确认缺陷、OPEN NCR、进行中 CAPA、已关闭 CAPA。
- 说明图表区域：批次状态分布、检测结果状态分布、缺陷类别分布、NCR / CAPA 状态分布。

预期结果：

- Dashboard 显示真实后端统计数据。
- 如果某类数据为空，图表显示空状态而不是报错。

面试讲解话术：

> Dashboard 的作用是把质量闭环的当前状态集中展示出来。面试时我会先从这里说明系统不是单纯 CRUD，而是围绕批次、检测结果、复核、NCR 和 CAPA 的质量状态做追踪和统计。

## 4. Step 2：查看批次列表和批次详情

页面路径：

```text
http://localhost:5173/batches
http://localhost:5173/batches/{id}
```

操作方式：

- 在批次列表按批次号或状态筛选。
- 点击“查看详情”进入批次详情。
- 在批次详情中查看该批次下的检测任务。

预期结果：

- 批次列表展示不同质量状态。
- 批次详情能关联展示该批次下的检测任务。

面试讲解话术：

> 批次是质量追溯的业务入口。后续检测任务、检测结果、人工复核、NCR 和 CAPA 都能回溯到批次，因此系统最终能回答“哪个批次出现了什么质量问题，以及问题是否已经关闭”。

## 5. Step 3：查看检测任务

页面路径：

```text
http://localhost:5173/inspection-tasks
```

操作方式：

- 按 taskNo、batchId 或 status 查询检测任务。
- 点击“查看检测结果”，跳转到检测结果列表并带上 taskId 查询条件。

预期结果：

- 检测任务列表展示任务编号、批次、模型名称、模型版本和任务状态。
- 跳转后检测结果列表只展示该任务下的检测结果。

面试讲解话术：

> 检测任务把生产批次和一次模型检测执行关联起来。第一版不做在线推理，而是通过 YOLO JSON 导入模拟模型结果进入业务系统，这样重点会落在业务闭环而不是模型训练上。

## 6. Step 4：查看检测结果列表

页面路径：

```text
http://localhost:5173/detections
```

操作方式：

- 使用 taskId、imageId、className 或 status 筛选检测结果。
- 点击“只看待复核”，定位 `PENDING_REVIEW` 检测结果。
- 观察列表中的类别、置信度、bbox 坐标和复核状态。

预期结果：

- 检测结果列表展示 YOLO JSON 导入后的结构化检测框数据。
- `PENDING_REVIEW` 记录可以继续进入人工复核。

面试讲解话术：

> 这里展示的是模型输出进入业务系统后的结构化结果，包括类别、置信度和 bbox 坐标。模型结果不会直接变成质量结论，而是先进入待复核状态。

## 7. Step 5：点击检测结果详情，展示图片 bbox 可视化

页面路径：

```text
http://localhost:5173/detections/{id}
```

操作方式：

- 在检测结果列表点击“查看详情”。
- 查看顶部基础信息：检测结果 ID、taskId、imageId、imageName、className、confidence、status、bbox 坐标、createdTime。
- 查看图片区域中叠加的 bbox 框，以及框附近的类别和置信度标签。

预期结果：

- 页面显示工业陶瓷表面样例图。
- bbox 坐标按 `1280 x 960` 逻辑尺寸映射到当前显示图片。
- bbox 缺失或越界时页面不会崩溃，会显示空提示。

面试讲解话术：

> Phase 2 增加了检测结果详情页，让检测框不再只是表格里的数字，而是能叠加到图片上展示。虽然这里使用演示图，但它已经表达了工业视觉质检最核心的信息：缺陷位置、类别、置信度和复核状态。

## 8. Step 6：对 PENDING_REVIEW 检测结果进行人工复核

页面路径：

```text
http://localhost:5173/detections/{id}
```

操作方式：

- 确认详情页状态为 `PENDING_REVIEW`。
- 点击“人工复核”。
- 保持 reviewerId 默认值 `2`。
- 选择复核结果：`CONFIRMED_DEFECT`、`FALSE_POSITIVE` 或 `NEED_RECHECK`。
- 填写复核备注并提交。

预期结果：

- 提交成功后刷新详情数据。
- 检测结果状态更新为对应复核结果。
- 如果选择 `CONFIRMED_DEFECT`，该结果后续可以进入 NCR。

面试讲解话术：

> 人工复核是模型结果和质量结论之间的隔离层。这样做可以避免模型误检直接进入质量流程，也能保留模型证据和人工判断两个层次的数据。

## 9. Step 7：从复核记录创建 NCR

页面路径：

```text
http://localhost:5173/reviews
```

操作方式：

- 筛选或找到 `reviewResult = CONFIRMED_DEFECT` 的复核记录。
- 点击“创建 NCR”。
- 使用默认 `NCR-FE-{时间戳}` 编号。
- 选择严重度，填写问题描述并提交。

预期结果：

- 新增 NCR 记录。
- NCR 状态为 `OPEN`。
- 对应批次状态变为 `NCR_OPEN`。

面试讲解话术：

> NCR 是确认缺陷后的正式不合格记录。系统要求从人工确认的复核记录创建 NCR，而不是从模型结果直接创建 NCR，这样更符合质量管理的审慎流程。

## 10. Step 8：从 NCR 创建 CAPA

页面路径：

```text
http://localhost:5173/ncrs
```

操作方式：

- 找到 `OPEN` 状态 NCR。
- 点击“创建 CAPA”。
- 使用默认 `CAPA-FE-{时间戳}` 编号。
- 填写负责人、根因分析、纠正措施、预防措施和计划完成日期。
- 提交后跳转到 CAPA 页面。

预期结果：

- 新增 CAPA 记录。
- CAPA 状态为 `IN_PROGRESS`。
- NCR 状态变为 `CAPA_CREATED`。
- 批次状态变为 `CAPA_OPEN`。

面试讲解话术：

> CAPA 是质量闭环的整改阶段。创建 CAPA 时系统会同时更新 NCR 和批次状态，这里用事务保证跨表状态一致，避免出现单据创建了但批次状态没有同步的问题。

## 11. Step 9：关闭 CAPA，核对 NCR 和批次状态同步关闭

页面路径：

```text
http://localhost:5173/capas
http://localhost:5173/ncrs
http://localhost:5173/batches
```

操作方式：

- 在 CAPA 页面找到新建 CAPA。
- 点击“编辑”，补充或确认根因、纠正措施、预防措施和验证结果。
- 点击“待验证”，状态变为 `PENDING_VERIFY`。
- 点击“关闭”，状态变为 `CLOSED`。
- 回到 NCR 页面核对 NCR 状态。
- 回到批次页面核对批次状态。

预期结果：

- CAPA 状态为 `CLOSED`。
- NCR 状态同步为 `CLOSED`。
- 批次状态同步为 `CLOSED`。

面试讲解话术：

> 关闭 CAPA 是整个质量闭环的终点。系统在一个事务里关闭 CAPA、关闭 NCR，并关闭批次质量状态，体现了质量业务中的跨表一致性控制。

## 12. 收尾总结话术

> 这条链路从 Dashboard 的质量概览开始，到批次、检测任务、检测结果、bbox 可视化、人工复核、NCR 和 CAPA，最终回写批次状态。它展示的是工业视觉检测结果进入企业质量体系后的完整闭环，而不是单纯的检测框展示或普通后台管理。
