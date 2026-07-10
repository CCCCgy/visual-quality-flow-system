# 代码结构与运行流程导读

本文用于帮助快速理解 `visual-quality-flow-system` 项目的代码结构、前后端调用关系、数据库关系和核心质量闭环流程。它不是需求文档，而是面向代码阅读、复习和面试讲解的 walkthrough。

## 一、项目整体结构

项目根目录主要包含以下部分：

- `backend/`：Spring Boot 后端工程。负责 REST API、业务校验、状态流转、MyBatis-Plus 数据访问、统一响应、异常处理、Knife4j 接口文档和后端自动化测试。
- `frontend/`：Vue 3 前端工程。负责管理后台页面、路由、Axios 请求封装、Element Plus UI、Dashboard ECharts 图表、检测结果 bbox 可视化页面。
- `sample-data/`：原始数据库初始化和演示数据。包含 `init_schema.sql`、`sample_seed_data.sql`、`sample_yolo_detection.json`，用于手动初始化 MySQL 和演示 YOLO JSON 导入。
- `docker/`：Docker MySQL 初始化脚本目录。`docker/mysql/init/01_init_schema.sql` 和 `02_sample_seed_data.sql` 是从 `sample-data` 复制来的初始化脚本，用于 Docker Compose 首次启动数据库时自动执行。
- `docker-compose.yml`：一键启动 MySQL 8 的 Compose 配置，服务名为 `mysql`，数据库名为 `visual_qms`，使用 named volume 保存数据。
- `docs/`：项目说明文档。包括数据库设计、API 设计、状态机、演示脚本、面试话术，以及本文代码导读。
- `screenshots/`：作品集展示截图，例如 Dashboard 质量看板、检测结果 bbox 可视化详情页、批次、NCR、CAPA 等页面截图。

整体上，项目是一个典型的前后端分离原型：

```text
Vue 页面
-> frontend/src/api/*.js
-> Vite /api 代理
-> Spring Boot Controller
-> Service / ServiceImpl
-> Mapper
-> MySQL 表
```

## 二、后端代码结构

后端主包路径为：

```text
backend/src/main/java/com/example/visualqms
```

### 1. Application 启动类

- `VisualQmsApplication`

这是 Spring Boot 启动入口，负责启动整个后端应用。运行后会扫描同包及子包下的 Controller、Service、Mapper、配置类等组件。

### 2. Controller 层

Controller 负责接收 HTTP 请求、参数校验、调用 Service，并用统一 `Result` 返回。它不写核心业务逻辑。

主要类：

- `ProductionBatchController`：批次接口，路径 `/api/batches`，提供创建、分页查询、详情、更新、状态更新。
- `InspectionTaskController`：检测任务接口，路径 `/api/inspection-tasks`，提供创建、分页查询、详情、按批次查询、状态更新。
- `DetectionController`：检测结果接口，路径 `/api/detections`，提供 YOLO JSON 导入、检测结果分页、检测结果详情、bbox 可视化详情。
- `ReviewRecordController`：人工复核接口，路径 `/api/reviews`，提供创建复核、分页查询、详情、按检测结果查询复核记录。
- `NcrRecordController`：NCR 接口，路径 `/api/ncrs`，提供创建 NCR、分页查询、详情、按复核记录查询、状态更新。
- `CapaRecordController`：CAPA 接口，路径 `/api/capas`，提供创建 CAPA、分页查询、详情、按 NCR 查询、更新、状态更新。
- `DashboardController`：Dashboard 统计接口，路径 `/api/dashboard`，提供 summary 和各类状态分布统计。
- `HealthController`：健康检查接口。

### 3. Service / ServiceImpl 层

Service 接口定义业务能力，ServiceImpl 负责真正的业务规则、状态流转、事务控制和 Mapper 调用。

主要接口和实现：

- `ProductionBatchService` / `ProductionBatchServiceImpl`
  - 创建批次时默认状态为 `CREATED`。
  - 校验 `batchNo` 唯一。
  - 已关闭批次不能再修改或切换到其他状态。

- `InspectionTaskService` / `InspectionTaskServiceImpl`
  - 创建检测任务时默认状态为 `CREATED`。
  - 校验 `taskNo` 唯一。
  - 已关闭批次不能创建检测任务。
  - 可按批次查询任务列表。

- `DetectionImportService` / `DetectionImportServiceImpl`
  - 负责 `POST /api/detections/import-json`。
  - 解析 YOLO JSON，创建或复用 `inspection_image`。
  - 将每个 box 写入 `detection_result`。
  - 检测结果默认状态为 `PENDING_REVIEW`。
  - 图片状态更新为 `DETECTED`，任务状态更新为 `WAIT_REVIEW`。
  - 方法使用 `@Transactional`，导入过程失败会回滚。

- `DetectionResultService` / `DetectionResultServiceImpl`
  - 查询检测结果分页。
  - 获取普通检测结果详情。
  - 获取 bbox 可视化详情：根据 `detection_result.id` 查检测结果，再根据 `image_id` 查 `inspection_image`，组合成 `DetectionVisualDetailVO`。

- `ReviewRecordService` / `ReviewRecordServiceImpl`
  - 只允许 `PENDING_REVIEW` 的检测结果被复核。
  - 复核结果允许 `CONFIRMED_DEFECT`、`FALSE_POSITIVE`、`NEED_RECHECK`。
  - 创建复核记录后，同步更新 `detection_result.status`。
  - 一个检测结果只能有一条复核记录。

- `NcrRecordService` / `NcrRecordServiceImpl`
  - 只允许 `CONFIRMED_DEFECT` 的复核记录创建 NCR。
  - 创建 NCR 时写入批次、任务、检测结果、复核记录等追溯字段。
  - NCR 默认状态为 `OPEN`。
  - 创建 NCR 后同步把批次状态更新为 `NCR_OPEN`。
  - 创建过程使用 `@Transactional`。

- `CapaRecordService` / `CapaRecordServiceImpl`
  - 只允许 `OPEN` 状态 NCR 创建 CAPA。
  - 创建 CAPA 后，CAPA 状态为 `IN_PROGRESS`。
  - 同步把 NCR 更新为 `CAPA_CREATED`，把批次更新为 `CAPA_OPEN`。
  - 关闭 CAPA 时，如果目标状态是 `CLOSED`，同步关闭 NCR 和批次。
  - 创建 CAPA、关闭 CAPA 使用 `@Transactional`，保证跨表状态一致。

- `DashboardService` / `DashboardServiceImpl`
  - 统计批次数、任务数、检测结果数、待复核数、确认缺陷数、OPEN NCR、进行中 CAPA、已关闭 CAPA。
  - 按 `status` 或 `class_name` 分组统计，用于前端图表。

### 4. Mapper 层

Mapper 继承 MyBatis-Plus 的 `BaseMapper<T>`，负责数据库访问。

主要类：

- `ProductionBatchMapper` -> `production_batch`
- `InspectionTaskMapper` -> `inspection_task`
- `InspectionImageMapper` -> `inspection_image`
- `DetectionResultMapper` -> `detection_result`
- `ReviewRecordMapper` -> `review_record`
- `NcrRecordMapper` -> `ncr_record`
- `CapaRecordMapper` -> `capa_record`

部分 Mapper 还提供用户存在性校验方法，例如复核人、NCR 创建人、CAPA 负责人是否存在于 `sys_user`。

### 5. Entity 层

Entity 与数据库表对应，使用 MyBatis-Plus 注解指定表名和主键。

主要类：

- `ProductionBatch` -> `production_batch`
- `InspectionTask` -> `inspection_task`
- `InspectionImage` -> `inspection_image`
- `DetectionResult` -> `detection_result`
- `ReviewRecord` -> `review_record`
- `NcrRecord` -> `ncr_record`
- `CapaRecord` -> `capa_record`

注意：`sys_user` 在本项目里用于演示用户存在性校验，但没有单独做完整用户管理模块。

### 6. DTO 层

DTO 是请求参数对象，用于接收前端或测试传来的 JSON。

典型类：

- `ProductionBatchCreateDTO`、`ProductionBatchUpdateDTO`、`ProductionBatchStatusUpdateDTO`
- `InspectionTaskCreateDTO`、`InspectionTaskStatusUpdateDTO`
- `DetectionImportDTO`、`YoloDetectionJsonDTO`、`YoloBoxDTO`
- `ReviewCreateDTO`
- `NcrCreateDTO`、`NcrStatusUpdateDTO`
- `CapaCreateDTO`、`CapaUpdateDTO`、`CapaStatusUpdateDTO`

DTO 中使用 Jakarta Validation 注解，例如 `@NotBlank`、`@NotNull`、`@Min`，Controller 上通过 `@Valid` 触发校验。

### 7. VO 层

VO 是接口返回给前端的数据对象。

典型类：

- `ProductionBatchVO`
- `InspectionTaskVO`
- `DetectionResultVO`
- `DetectionVisualDetailVO`
- `DetectionImportResultVO`
- `ReviewRecordVO`
- `NcrRecordVO`
- `CapaRecordVO`
- `DashboardSummaryVO`
- `StatusCountVO`
- `ClassCountVO`

例如 `DetectionVisualDetailVO` 会把检测结果字段和图片字段组合到一起，给前端详情页绘制 bbox 使用。

### 8. common Result

- `Result<T>`：统一响应结构，字段为 `code`、`message`、`data`。成功时 `code=200`。
- `PageResult<T>`：统一分页结构，封装 `total`、`pageNo`、`pageSize`、`records`。

前端 `request.js` 会读取 `Result.code`，如果不是 200，就弹出错误提示并 reject。

### 9. exception

- `BizException`：业务异常，带 `code` 和 `message`。
- `GlobalExceptionHandler`：统一异常处理。
  - `BizException` 返回业务错误。
  - `MethodArgumentNotValidException` 返回参数校验错误。
  - 其他异常统一返回 `Internal server error`。

### 10. config

- `MybatisPlusConfig`：配置 MyBatis-Plus 分页插件等能力。
- `Knife4jConfig`：配置 Knife4j / OpenAPI 文档分组和接口文档信息。

## 三、前端代码结构

前端主目录：

```text
frontend/src
```

### 1. main.js

`main.js` 创建 Vue 应用：

```js
createApp(App).use(router).use(ElementPlus).mount('#app')
```

它引入：

- `ElementPlus`
- Element Plus 样式
- 全局样式 `styles/global.css`
- 根组件 `App.vue`
- 路由 `router/index.js`

### 2. router/index.js

路由统一挂在 `MainLayout` 下：

- `/` -> `DashboardView`
- `/batches` -> `BatchListView`
- `/batches/:id` -> `BatchDetailView`
- `/inspection-tasks` -> `InspectionTaskListView`
- `/detections` -> `DetectionResultListView`
- `/detections/:id` -> `DetectionResultDetailView`
- `/reviews` -> `ReviewListView`
- `/ncrs` -> `NcrListView`
- `/capas` -> `CapaListView`

### 3. layout

- `MainLayout.vue`

负责整体后台布局：

- 左侧菜单：Dashboard、批次管理、检测任务、检测结果、人工复核、NCR、CAPA。
- 顶部标题：工业视觉检测结果复核与质量闭环管理系统。
- 中间通过 `<router-view />` 渲染具体页面。

### 4. views

主要页面职责：

- `DashboardView.vue`
  - 调用 `dashboardApi.js` 的 6 个统计接口。
  - 展示统计卡片：生产批次、检测任务、检测结果、待复核、确认缺陷、OPEN NCR、进行中 CAPA、已关闭 CAPA。
  - 使用 ECharts 展示批次状态、检测状态、缺陷类别、NCR 状态、CAPA 状态图表。

- `BatchListView.vue`
  - 查询生产批次列表。
  - 支持批次号、状态筛选。
  - 进入批次详情。

- `BatchDetailView.vue`
  - 查看批次详情。
  - 查询该批次下检测任务。

- `InspectionTaskListView.vue`
  - 查询检测任务。
  - 支持按任务号、批次 ID、状态筛选。
  - 可跳转到检测结果列表，并携带 `taskId` 查询条件。

- `DetectionResultListView.vue`
  - 查询检测结果列表。
  - 展示类别、置信度、bbox 坐标、状态。
  - 对 `PENDING_REVIEW` 结果打开人工复核弹窗。
  - 可点击“查看详情”进入 bbox 可视化详情页。

- `DetectionResultDetailView.vue`
  - 调用 `getDetectionVisualDetail(id)`。
  - 展示检测结果基础信息、图片名、类别、置信度、bbox 坐标。
  - 使用固定样例图 `/demo-images/demo-ceramic.svg`，按 `1280 x 960` 逻辑尺寸映射 bbox。
  - 如果状态是 `PENDING_REVIEW`，提供人工复核按钮。

- `ReviewListView.vue`
  - 查询复核记录。
  - 对 `CONFIRMED_DEFECT` 复核记录创建 NCR。

- `NcrListView.vue`
  - 查询 NCR 记录。
  - 对 `OPEN` NCR 创建 CAPA。
  - 可关闭或取消 NCR。

- `CapaListView.vue`
  - 查询 CAPA 记录。
  - 支持编辑 CAPA 内容。
  - 支持状态流转到 `PENDING_VERIFY`、`CLOSED`、`CANCELLED`。

### 5. api

`frontend/src/api` 封装后端接口：

- `request.js`
  - Axios 实例，`baseURL=/api`。
  - 统一解析后端 `Result{code,message,data}`。
  - 非 200 时用 Element Plus `ElMessage.error` 提示。

- `batchApi.js`
  - `getBatchPage`
  - `getBatchDetail`

- `taskApi.js`
  - `getInspectionTaskPage`
  - `getInspectionTaskDetail`
  - `getInspectionTasksByBatch`

- `detectionApi.js`
  - `getDetectionResultPage`
  - `getDetectionResultDetail`
  - `getDetectionVisualDetail`

- `reviewApi.js`
  - `createReview`
  - `getReviewPage`
  - `getReviewDetail`
  - `getReviewByDetection`

- `ncrApi.js`
  - `createNcr`
  - `getNcrPage`
  - `getNcrDetail`
  - `getNcrByReview`
  - `updateNcrStatus`

- `capaApi.js`
  - `createCapa`
  - `getCapaPage`
  - `getCapaDetail`
  - `getCapaByNcr`
  - `updateCapa`
  - `updateCapaStatus`

- `dashboardApi.js`
  - `getDashboardSummary`
  - `getBatchStatusStats`
  - `getDetectionStatusStats`
  - `getDefectClassStats`
  - `getNcrStatusStats`
  - `getCapaStatusStats`

### 6. ECharts

ECharts 只用于 `DashboardView.vue`。

页面中通过：

```js
import * as echarts from 'echarts'
```

初始化饼图和柱状图。图表数据来自 Dashboard 后端统计接口。

### 7. Element Plus

Element Plus 是前端主要 UI 组件库，用在：

- `el-container`、`el-aside`、`el-main` 布局。
- `el-menu` 菜单。
- `el-table` 表格。
- `el-dialog` 弹窗。
- `el-form` 表单。
- `el-input`、`el-select`、`el-input-number` 等输入控件。
- `el-tag` 状态标签。
- `ElMessage` 消息提示。

## 四、核心业务流程调用链

下面按“前端页面 -> api.js -> Controller -> Service -> Mapper -> 数据表”的形式说明核心调用链。

### 1. 创建生产批次

当前前端主要展示批次查询和详情，创建批次接口主要由后端、Knife4j 或测试调用。

```text
外部请求 / 测试
-> POST /api/batches
-> ProductionBatchController.createBatch
-> ProductionBatchService.createBatch
-> ProductionBatchServiceImpl.createBatch
-> ProductionBatchMapper
-> production_batch
```

关键逻辑：

- 请求 DTO：`ProductionBatchCreateDTO`。
- 校验 `batchNo` 唯一。
- 新批次状态设为 `CREATED`。
- 返回 `ProductionBatchVO`。

### 2. 创建检测任务

```text
外部请求 / 测试
-> POST /api/inspection-tasks
-> InspectionTaskController.createTask
-> InspectionTaskService.createTask
-> InspectionTaskServiceImpl.createTask
-> InspectionTaskMapper + ProductionBatchMapper
-> inspection_task + production_batch
```

关键逻辑：

- 请求 DTO：`InspectionTaskCreateDTO`。
- 校验 `taskNo` 唯一。
- 校验批次存在，且批次不能是 `CLOSED`。
- 新任务状态设为 `CREATED`。

### 3. 导入 YOLO JSON

```text
Knife4j / 测试
-> POST /api/detections/import-json
-> DetectionController.importYoloJson
-> DetectionImportService.importYoloJson
-> DetectionImportServiceImpl.importYoloJson
-> InspectionTaskMapper + InspectionImageMapper + DetectionResultMapper
-> inspection_task + inspection_image + detection_result
```

关键逻辑：

- 请求 DTO：`DetectionImportDTO`，内部包含 `YoloDetectionJsonDTO` 和 `YoloBoxDTO`。
- 校验任务存在且不是 `CLOSED` / `CANCELLED`。
- 根据 `sourceName` 创建或复用 `inspection_image`。
- 每个 box 写入一条 `detection_result`。
- `detection_result.status = PENDING_REVIEW`。
- `inspection_image.status = DETECTED`。
- `inspection_task.status = WAIT_REVIEW`。

### 4. 查询检测结果

```text
DetectionResultListView.vue
-> detectionApi.getDetectionResultPage
-> GET /api/detections
-> DetectionController.pageDetectionResults
-> DetectionResultService.pageDetectionResults
-> DetectionResultServiceImpl.pageDetectionResults
-> DetectionResultMapper
-> detection_result
```

可按以下条件筛选：

- `taskId`
- `imageId`
- `className`
- `status`
- `pageNo`
- `pageSize`

返回 `PageResult<DetectionResultVO>`。

### 5. 人工复核

```text
DetectionResultListView.vue / DetectionResultDetailView.vue
-> reviewApi.createReview
-> POST /api/reviews
-> ReviewRecordController.createReview
-> ReviewRecordService.createReview
-> ReviewRecordServiceImpl.createReview
-> ReviewRecordMapper + DetectionResultMapper
-> review_record + detection_result
```

关键逻辑：

- 请求 DTO：`ReviewCreateDTO`。
- 只允许 `PENDING_REVIEW` 的检测结果复核。
- 复核结果只能是 `CONFIRMED_DEFECT`、`FALSE_POSITIVE`、`NEED_RECHECK`。
- 创建 `review_record`。
- 同步更新 `detection_result.status = reviewResult`。
- 一个检测结果只能复核一次。

### 6. 创建 NCR

```text
ReviewListView.vue
-> ncrApi.createNcr
-> POST /api/ncrs
-> NcrRecordController.createNcr
-> NcrRecordService.createNcr
-> NcrRecordServiceImpl.createNcr
-> ReviewRecordMapper + DetectionResultMapper + InspectionTaskMapper + ProductionBatchMapper + NcrRecordMapper
-> review_record + detection_result + inspection_task + production_batch + ncr_record
```

关键逻辑：

- 请求 DTO：`NcrCreateDTO`。
- 只允许 `review_result = CONFIRMED_DEFECT` 的复核记录创建 NCR。
- 校验复核记录、检测结果、检测任务、批次之间的一致性。
- 新 NCR 状态设为 `OPEN`。
- 同步更新 `production_batch.status = NCR_OPEN`。
- 一个复核记录只能创建一个 NCR。

### 7. 创建 CAPA

```text
NcrListView.vue
-> capaApi.createCapa
-> POST /api/capas
-> CapaRecordController.createCapa
-> CapaRecordService.createCapa
-> CapaRecordServiceImpl.createCapa
-> NcrRecordMapper + CapaRecordMapper + ProductionBatchMapper
-> ncr_record + capa_record + production_batch
```

关键逻辑：

- 请求 DTO：`CapaCreateDTO`。
- 只允许 `OPEN` NCR 创建 CAPA。
- 新 CAPA 状态设为 `IN_PROGRESS`。
- 同步更新 `ncr_record.status = CAPA_CREATED`。
- 同步更新 `production_batch.status = CAPA_OPEN`。
- 一个 NCR 只能创建一个 CAPA。

### 8. 关闭 CAPA

```text
CapaListView.vue
-> capaApi.updateCapaStatus
-> PATCH /api/capas/{id}/status
-> CapaRecordController.updateCapaStatus
-> CapaRecordService.updateCapaStatus
-> CapaRecordServiceImpl.updateCapaStatus
-> CapaRecordMapper + NcrRecordMapper + ProductionBatchMapper
-> capa_record + ncr_record + production_batch
```

关键逻辑：

- 请求 DTO：`CapaStatusUpdateDTO`。
- 当目标状态是 `CLOSED`：
  - 更新 `capa_record.status = CLOSED`，写入 `closed_time`。
  - 更新 `ncr_record.status = CLOSED`，写入 `closed_time`。
  - 更新 `production_batch.status = CLOSED`。
- 这个方法有 `@Transactional`，三张表状态必须一起成功。

### 9. Dashboard 统计

```text
DashboardView.vue
-> dashboardApi.js
-> GET /api/dashboard/*
-> DashboardController
-> DashboardService
-> DashboardServiceImpl
-> ProductionBatchMapper / InspectionTaskMapper / DetectionResultMapper / NcrRecordMapper / CapaRecordMapper
-> production_batch / inspection_task / detection_result / ncr_record / capa_record
```

Dashboard 接口：

- `/api/dashboard/summary`
- `/api/dashboard/batch-status`
- `/api/dashboard/detection-status`
- `/api/dashboard/defect-class`
- `/api/dashboard/ncr-status`
- `/api/dashboard/capa-status`

统计内容包括总数、状态分布和缺陷类别分布。

### 10. 检测结果 bbox 可视化详情页

```text
DetectionResultDetailView.vue
-> detectionApi.getDetectionVisualDetail
-> GET /api/detections/{id}/visual-detail
-> DetectionController.getDetectionVisualDetail
-> DetectionResultService.getDetectionVisualDetail
-> DetectionResultServiceImpl.getDetectionVisualDetail
-> DetectionResultMapper + InspectionImageMapper
-> detection_result + inspection_image
```

关键逻辑：

- 后端返回检测结果字段和图片字段：`imageName`、`imageUri`、`className`、`confidence`、bbox 坐标、状态等。
- 前端使用样例图 `/demo-images/demo-ceramic.svg`。
- 前端按 `1280 x 960` 逻辑尺寸将 bbox 坐标映射到当前显示区域。
- 如果状态为 `PENDING_REVIEW`，详情页显示人工复核入口。

## 五、数据库表关系

本项目使用逻辑外键方式建模，SQL 中主要通过 ID 字段和索引表达关系，没有定义数据库级外键约束。

### 1. sys_user

`sys_user` 是演示用户表。

用途：

- `production_batch.created_by` 指向创建批次的用户。
- `inspection_task.created_by` 指向创建任务的用户。
- `review_record.reviewer_id` 指向复核人。
- `ncr_record.created_by` 指向 NCR 创建人。
- `capa_record.owner_id` 指向 CAPA 负责人。

### 2. production_batch

生产批次表，是质量追溯的入口。

一条批次可以关联多条检测任务：

```text
production_batch.id = inspection_task.batch_id
```

### 3. inspection_task

检测任务表，代表某个批次下的一次检测执行。

一条任务可以关联多张检测图片：

```text
inspection_task.id = inspection_image.task_id
```

一条任务也可以关联多条检测结果：

```text
inspection_task.id = detection_result.task_id
```

### 4. inspection_image

检测图片表，记录图片名称、路径、所属任务和批次。

一张图片可以关联多条检测结果：

```text
inspection_image.id = detection_result.image_id
```

### 5. detection_result

检测结果表，保存 YOLO 输出的类别、置信度、bbox 坐标、状态和 raw payload。

一条检测结果最多对应一条人工复核记录：

```text
detection_result.id = review_record.detection_result_id
```

`review_record.detection_result_id` 上有唯一索引，保证一个检测结果只能复核一次。

### 6. review_record

人工复核记录表，保存复核人、复核结果和备注。

一条确认缺陷的复核记录最多创建一条 NCR：

```text
review_record.id = ncr_record.review_id
```

`ncr_record.review_id` 上有唯一索引，保证一个复核记录只能生成一个 NCR。

### 7. ncr_record

NCR 不合格记录表，记录问题描述、严重度、状态，并保存来源批次、任务、检测结果、复核记录。

一条 NCR 最多创建一条 CAPA：

```text
ncr_record.id = capa_record.ncr_id
```

`capa_record.ncr_id` 上有唯一索引，保证一个 NCR 只能生成一个 CAPA。

### 8. capa_record

CAPA 整改记录表，记录根因、纠正措施、预防措施、验证结果、负责人、状态和关闭时间。

完整链路可以理解为：

```text
sys_user
  ├─ 创建 production_batch
  ├─ 创建 inspection_task
  ├─ 复核 review_record
  ├─ 创建 ncr_record
  └─ 负责 capa_record

production_batch
→ inspection_task
→ inspection_image
→ detection_result
→ review_record
→ ncr_record
→ capa_record
```

## 六、关键状态流转

### 1. production_batch.status

定义在表注释和 `ProductionBatchServiceImpl` 中：

- `CREATED`：批次刚创建。
- `INSPECTING`：检测中状态，可手动更新。
- `NCR_OPEN`：创建 NCR 后自动更新。
- `CAPA_OPEN`：创建 CAPA 后自动更新。
- `CLOSED`：关闭 CAPA 后自动同步关闭。

关键点：

- 创建批次默认 `CREATED`。
- 创建 NCR 后批次变为 `NCR_OPEN`。
- 创建 CAPA 后批次变为 `CAPA_OPEN`。
- 关闭 CAPA 后批次变为 `CLOSED`。
- `CLOSED` 批次不能再修改或切换回其他状态。

### 2. inspection_task.status

定义在表注释和 `InspectionTaskServiceImpl` 中：

- `CREATED`：任务创建。
- `WAIT_REVIEW`：YOLO JSON 导入后，等待复核。
- `REVIEWED`：已复核，当前主要作为允许状态保留。
- `CLOSED`：任务关闭。
- `CANCELLED`：任务取消。

关键点：

- 创建任务默认 `CREATED`。
- 导入 YOLO JSON 后自动变为 `WAIT_REVIEW`。
- `CLOSED` / `CANCELLED` 是终态，不能随意切换到其他状态。

### 3. inspection_image.status

定义在表注释和 `DetectionImportServiceImpl` 中：

- `PENDING`：待检测，表默认值。
- `DETECTED`：已经导入检测结果。
- `REVIEWED`：已复核，当前主要作为状态枚举保留。

关键点：

- YOLO JSON 导入时创建或复用图片，并把图片状态设为 `DETECTED`。

### 4. detection_result.status

定义在表注释、`DetectionImportServiceImpl`、`ReviewRecordServiceImpl` 中：

- `PENDING_REVIEW`：待人工复核。
- `CONFIRMED_DEFECT`：人工确认缺陷。
- `FALSE_POSITIVE`：误检。
- `NEED_RECHECK`：需要复检。

关键点：

- YOLO JSON 导入后默认 `PENDING_REVIEW`。
- 人工复核后，状态同步改为复核结果。
- 只有 `PENDING_REVIEW` 可以被复核。
- 只有 `CONFIRMED_DEFECT` 的复核记录可以创建 NCR。

### 5. review_record.review_result

人工复核结论：

- `CONFIRMED_DEFECT`
- `FALSE_POSITIVE`
- `NEED_RECHECK`

它不是单纯的状态字段，而是人工判断结果。系统把它单独放在 `review_record` 中，是为了保留模型输出和人工判断两个层次。

### 6. ncr_record.status

定义在表注释和 `NcrRecordServiceImpl`、`CapaRecordServiceImpl` 中：

- `OPEN`：NCR 刚创建，待处理。
- `CAPA_CREATED`：已创建 CAPA。
- `CLOSED`：已关闭。
- `CANCELLED`：已取消。

关键点：

- 创建 NCR 默认 `OPEN`。
- 创建 CAPA 后自动变为 `CAPA_CREATED`。
- 关闭 CAPA 后自动变为 `CLOSED`。
- `CLOSED` / `CANCELLED` 是终态。

### 7. capa_record.status

定义在表注释和 `CapaRecordServiceImpl` 中：

- `PENDING_ANALYSIS`：待分析，表默认值。
- `IN_PROGRESS`：进行中，创建 CAPA 时设置。
- `PENDING_VERIFY`：待验证。
- `CLOSED`：关闭。
- `CANCELLED`：取消。

关键点：

- 创建 CAPA 时服务层直接设为 `IN_PROGRESS`。
- 关闭 CAPA 时会同步关闭 NCR 和批次。
- `CLOSED` / `CANCELLED` 是终态。

### 为什么关闭 CAPA 时要同步关闭 NCR 和 batch

CAPA 是质量闭环的整改和验证阶段。业务含义上，CAPA 关闭代表整改措施已经完成并通过验证，因此：

- CAPA 自己要变成 `CLOSED`。
- 它来源的 NCR 表示的问题也应关闭，所以 `ncr_record.status` 变成 `CLOSED`。
- 该质量问题追溯到的生产批次也完成处置，所以 `production_batch.status` 变成 `CLOSED`。

如果只关闭 CAPA，不同步关闭 NCR 和批次，就会出现：

```text
CAPA 已关闭
但 NCR 仍显示 CAPA_CREATED
批次仍显示 CAPA_OPEN
```

这会造成质量状态不一致。`CapaRecordServiceImpl.updateCapaStatus` 使用 `@Transactional`，并在 `closeNcrAndBatch` 中同步更新三张表，保证闭环终点一致。

## 七、自动化测试如何串起主流程

测试类：

```text
backend/src/test/java/com/example/visualqms/CoreFlowIntegrationTest.java
```

使用：

- JUnit 5
- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `MockMvc`
- `ObjectMapper`

### 1. dashboardApisShouldReturnSuccess 测了什么

这个测试依次调用 6 个 Dashboard 接口，并断言后端统一响应 `Result.code = 200`。

覆盖接口：

- `GET /api/dashboard/summary`
- `GET /api/dashboard/batch-status`
- `GET /api/dashboard/detection-status`
- `GET /api/dashboard/defect-class`
- `GET /api/dashboard/ncr-status`
- `GET /api/dashboard/capa-status`

它证明 Dashboard 的统计入口可以被正常访问，且返回结构符合统一 `Result` 约定。

### 2. completeQualityClosedLoopShouldWork 的调用顺序

这个测试使用当前时间戳生成唯一编号，避免和 seed 数据冲突。

调用顺序：

1. `POST /api/batches`
   - 创建生产批次。
   - 获取 `batchId`。

2. `POST /api/inspection-tasks`
   - 使用 `batchId` 创建检测任务。
   - 获取 `taskId`。

3. `POST /api/detections/import-json`
   - 导入一个包含 1 个 box 的 YOLO JSON。
   - box 内容包括 `classId=1`、`className=DS`、`confidence=0.85`、`bboxXyxy=[100,120,240,260]`。

4. `GET /api/detections?taskId={taskId}`
   - 查询刚导入的检测结果。
   - 获取 `detectionResultId`。

5. `POST /api/reviews`
   - 对检测结果进行人工复核。
   - `reviewResult=CONFIRMED_DEFECT`。
   - 获取 `reviewId`。

6. `POST /api/ncrs`
   - 基于复核记录创建 NCR。
   - 获取 `ncrId`。

7. `POST /api/capas`
   - 基于 NCR 创建 CAPA。
   - 获取 `capaId`。

8. `PATCH /api/capas/{capaId}/status`
   - 把 CAPA 状态更新为 `CLOSED`。

9. 分别读取：
   - `GET /api/capas/{capaId}`
   - `GET /api/ncrs/{ncrId}`
   - `GET /api/batches/{batchId}`

10. 断言：
   - CAPA status = `CLOSED`
   - NCR status = `CLOSED`
   - batch status = `CLOSED`

### 3. 为什么这个测试能证明主流程可回归

这个测试不是只测一个 Service 方法，而是通过 HTTP 接口串起完整主链路：

```text
批次
-> 检测任务
-> YOLO JSON 导入
-> 检测结果
-> 人工复核
-> NCR
-> CAPA
-> 状态同步关闭
```

它覆盖了多个 Controller、ServiceImpl、Mapper 和数据库表。一旦某个接口路径、DTO 字段、状态校验、事务同步或返回结构被破坏，测试就会在对应步骤失败，并通过断言信息提示是哪一步出问题。

需要注意的是：该测试依赖一个已经启动并初始化好的 MySQL 数据库。它不是内存数据库测试，也没有使用 Testcontainers。

## 八、面试理解版总结

这个项目不是普通 CRUD，它的核心是把“模型检测结果”接入“质量业务闭环”。YOLO 只负责输出候选缺陷，包括类别、置信度和 bbox 坐标；系统要做的是把这些结果保存成业务数据，再经过人工复核确认，只有确认缺陷才进入 NCR，不合格记录再推动 CAPA 整改，最后关闭 CAPA 时同步关闭 NCR 和批次状态。

所以这个项目真正要讲清楚的是三件事：

1. **追溯关系**：从生产批次到检测任务、检测图片、检测结果、复核记录、NCR、CAPA，每一步都能追溯来源。
2. **状态流转**：检测结果从 `PENDING_REVIEW` 到人工结论，NCR 从 `OPEN` 到 `CAPA_CREATED` 再到 `CLOSED`，批次状态也跟着质量处置进度变化。
3. **一致性控制**：创建 NCR、创建 CAPA、关闭 CAPA 都涉及多张表，必须用事务保证数据状态一起成功或一起回滚。

Dashboard 和 bbox 可视化让这个项目更适合展示：Dashboard 说明当前质量闭环状态，bbox 详情页说明视觉检测结果具体落在哪里。Docker Compose 和 MockMvc 测试则说明这个项目不只是页面演示，也考虑了初始化和回归验证。

它仍然是一个学习、作品集和面试展示用原型，不是完整 MES/QMS，也没有接入真实产线或在线模型推理。面试时可以重点表达：我做的是工业视觉检测结果进入质量管理流程后的业务建模、状态流转和全栈实现。
