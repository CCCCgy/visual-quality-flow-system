# 前后端演示脚本

本文用于面试、作品集录屏或现场演示。推荐先用前端页面展示主链路，再用 Knife4j 补充说明接口和状态流转。

## 1. 演示准备

### 启动后端

```bat
cd /d D:\Project_Portfolio\visual-quality-flow-system\backend
set MYSQL_PASSWORD=your_mysql_password
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
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

说明：示例编号如果重复，请替换日期或序号后重新执行。

## 2. 演示主线

```text
Dashboard
-> 创建 / 查看生产批次
-> 创建检测任务
-> 导入 YOLO JSON
-> 查看检测结果
-> 人工复核为 CONFIRMED_DEFECT
-> 创建 NCR
-> 创建 CAPA
-> 更新 / 关闭 CAPA
-> 验证批次、NCR、CAPA 同步关闭
```

## 3. Dashboard：说明系统定位

前端页面：

```text
http://localhost:5173/
```

演示动作：

- 打开 Dashboard。
- 指出系统主链路：生产批次 -> 检测任务 -> YOLO JSON 导入 -> 检测结果 -> 人工复核 -> NCR -> CAPA。

讲解话术：

> 这个项目不是做模型训练，而是解决模型检测结果进入企业质量管理后的流程问题。YOLO 只能输出框和类别，真实质检还需要人工复核、NCR 和 CAPA，最后形成批次质量状态闭环。

## 4. 批次管理：查看质量对象

前端页面：

```text
http://localhost:5173/batches
```

后端接口：

```text
GET /api/batches
GET /api/batches/{id}
```

演示动作：

- 在批次列表按批次号或状态筛选。
- 点击“查看详情”进入批次详情。
- 在批次详情查看“该批次下的检测任务”。

讲解话术：

> 批次是质量追溯的业务入口。后续检测任务、检测结果、复核、NCR 和 CAPA 都会回溯到批次，这样能回答“哪个批次出现了什么质量问题，以及问题是否已经关闭”。

## 5. 检测任务：查看模型检测任务

前端页面：

```text
http://localhost:5173/inspection-tasks
```

后端接口：

```text
GET /api/inspection-tasks
GET /api/inspection-tasks/by-batch/{batchId}
```

演示动作：

- 按 taskNo、batchId 或 status 查询检测任务。
- 点击“查看检测结果”，跳转到：

```text
http://localhost:5173/detections?taskId={taskId}
```

讲解话术：

> 检测任务把生产批次和一次模型检测执行关联起来。第一版不做在线推理，而是通过 YOLO JSON 导入模拟模型结果进入业务系统。

## 6. YOLO JSON 导入：用 Knife4j 演示

前端当前不提供导入表单，建议用 Knife4j 演示。

接口路径：

```text
POST /api/detections/import-json
```

示例请求：

```json
{
  "taskId": 2001,
  "yoloJson": {
    "sourceName": "demo_surface_show_001.jpg",
    "weightsName": "demo_model.pt",
    "visualizationName": "demo_surface_show_001_pred.jpg",
    "classNames": ["CK", "DS", "GS", "SS", "EC", "AC", "PH"],
    "parameters": {
      "conf": 0.25,
      "iou": 0.6,
      "imgsz": 800
    },
    "inferenceTimeMs": 128.5,
    "boxes": [
      {
        "classId": 1,
        "className": "DS",
        "confidence": 0.8123,
        "bboxXyxy": [120.50, 220.30, 188.90, 260.70]
      }
    ]
  }
}
```

预期结果：

- 写入 `inspection_image`。
- 写入 `detection_result`。
- 检测结果默认 `PENDING_REVIEW`。
- 检测任务状态变为 `WAIT_REVIEW`。

讲解话术：

> 这里是 AI 输出到业务数据的转换层。系统把 YOLO 的 bbox、类别和置信度映射成数据库中的检测结果，同时保留 raw_payload，方便后续追溯。

## 7. 检测结果：进入人工复核

前端页面：

```text
http://localhost:5173/detections
```

后端接口：

```text
GET /api/detections
POST /api/reviews
```

演示动作：

- 点击“只看待复核”。
- 找到 `PENDING_REVIEW` 的检测结果。
- 点击“人工复核”。
- 选择 `CONFIRMED_DEFECT`，填写备注后提交。
- 列表刷新后确认该检测结果状态变为 `CONFIRMED_DEFECT`。

讲解话术：

> 模型结果不直接进入质量结论，而是先进入待复核。人工复核把可疑检测框转成业务结论：真实缺陷、误检或需复检。只有真实缺陷才继续进入 NCR。

## 8. 复核记录：创建 NCR

前端页面：

```text
http://localhost:5173/reviews
```

后端接口：

```text
GET /api/reviews
POST /api/ncrs
```

演示动作：

- 筛选 `reviewResult = CONFIRMED_DEFECT`。
- 点击“创建 NCR”。
- 使用默认 `NCR-FE-{时间戳}` 编号。
- 选择严重度，填写问题描述，提交。
- 成功后跳转到 NCR 页面。

预期结果：

- 新增 NCR。
- NCR 状态为 `OPEN`。
- 对应批次状态变为 `NCR_OPEN`。

讲解话术：

> NCR 是确认缺陷后的正式不合格记录。它不是从模型直接生成，而是从人工确认的复核记录生成，这能避免误检污染质量流程。

## 9. NCR：创建 CAPA

前端页面：

```text
http://localhost:5173/ncrs
```

后端接口：

```text
GET /api/ncrs
POST /api/capas
PATCH /api/ncrs/{id}/status
```

演示动作：

- 找到 `OPEN` 状态 NCR。
- 点击“创建 CAPA”。
- 使用默认 `CAPA-FE-{时间戳}` 编号。
- 填写负责人、根因分析、纠正措施、预防措施和计划完成日期。
- 提交后跳转到 CAPA 页面。

预期结果：

- 新增 CAPA。
- CAPA 状态为 `IN_PROGRESS`。
- NCR 状态变为 `CAPA_CREATED`。
- 批次状态变为 `CAPA_OPEN`。

讲解话术：

> CAPA 是质量闭环的整改阶段。这里用事务保证 CAPA 创建、NCR 状态和批次状态同时更新，避免出现 CAPA 已建但批次状态没变的跨表不一致。

## 10. CAPA：编辑并关闭整改

前端页面：

```text
http://localhost:5173/capas
```

后端接口：

```text
GET /api/capas
PUT /api/capas/{id}
PATCH /api/capas/{id}/status
```

演示动作：

- 找到新建 CAPA。
- 点击“编辑”，补充根因、纠正措施、预防措施和验证结果。
- 点击“待验证”，状态变为 `PENDING_VERIFY`。
- 点击“关闭”，状态变为 `CLOSED`。

预期结果：

- CAPA 状态为 `CLOSED`。
- NCR 状态同步为 `CLOSED`。
- 批次状态同步为 `CLOSED`。

讲解话术：

> 关闭 CAPA 是整个质量闭环的终点。系统在一个事务里关闭 CAPA、关闭 NCR，并关闭批次质量状态，体现了质量业务中的跨表一致性控制。

## 11. 最终核对

推荐核对页面：

- `/batches`：确认批次状态为 `CLOSED`。
- `/ncrs`：确认 NCR 状态为 `CLOSED`。
- `/capas`：确认 CAPA 状态为 `CLOSED`。

收尾话术：

> 这条链路从生产批次开始，到模型检测结果导入，再到人工复核、NCR 和 CAPA，最终回写批次状态。它展示的是工业视觉检测结果进入企业质量体系后的完整闭环，而不是单纯的检测框展示。
