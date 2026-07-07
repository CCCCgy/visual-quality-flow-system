# API 设计

## 1. 统一返回结构

所有接口统一返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页接口返回 `PageResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "records": []
  }
}
```

常见业务错误：

- `400`：请求参数或业务状态不合法。
- `404`：业务对象不存在。
- `500`：未预期的服务端异常。

## 2. Batch 批次管理

### POST `/api/batches`

创建生产批次。

请求示例：

```json
{
  "batchNo": "BATCH-DEMO-20260707-001",
  "productCode": "PROD-DEMO-CERAMIC",
  "productName": "Demo Ceramic Surface Part",
  "plannedQuantity": 100,
  "createdBy": 1,
  "remark": "Demo batch for visual quality flow."
}
```

核心规则：

- `batchNo` 不能为空且不能重复。
- `productCode`、`productName`、`createdBy` 不能为空。
- `plannedQuantity` 不能小于 0。
- 创建后状态默认为 `CREATED`。

### GET `/api/batches`

分页查询批次。

查询参数：

- `batchNo`：模糊查询。
- `status`：精确查询。
- `pageNo`：页码。
- `pageSize`：每页条数。

### GET `/api/batches/{id}`

查询批次详情。批次不存在时返回业务错误。

### PUT `/api/batches/{id}`

更新批次基础信息。

可更新字段：

- `productCode`
- `productName`
- `plannedQuantity`
- `remark`

`CLOSED` 状态不允许修改基础信息。

### PATCH `/api/batches/{id}/status`

更新批次状态。

请求示例：

```json
{
  "status": "INSPECTING"
}
```

允许状态：

- `CREATED`
- `INSPECTING`
- `NCR_OPEN`
- `CAPA_OPEN`
- `CLOSED`

## 3. InspectionTask 检测任务管理

### POST `/api/inspection-tasks`

创建检测任务。

请求示例：

```json
{
  "taskNo": "TASK-DEMO-20260707-001",
  "batchId": 1001,
  "modelName": "surface-defect-yolo",
  "modelVersion": "demo-v1.0",
  "sourceType": "YOLO_JSON",
  "createdBy": 2
}
```

核心规则：

- `taskNo` 不能为空且不能重复。
- `batchId` 必须存在。
- 批次为 `CLOSED` 时不允许创建检测任务。
- 创建后状态默认为 `CREATED`。

### GET `/api/inspection-tasks`

分页查询检测任务。

查询参数：

- `taskNo`：模糊查询。
- `batchId`：精确查询。
- `status`：精确查询。
- `pageNo`
- `pageSize`

### GET `/api/inspection-tasks/{id}`

查询检测任务详情。

### GET `/api/inspection-tasks/by-batch/{batchId}`

根据批次查询检测任务列表。

### PATCH `/api/inspection-tasks/{id}/status`

更新检测任务状态。

允许状态：

- `CREATED`
- `WAIT_REVIEW`
- `REVIEWED`
- `CLOSED`
- `CANCELLED`

## 4. Detection 检测结果

### POST `/api/detections/import-json`

导入 YOLO JSON，生成 `inspection_image` 和 `detection_result`。

请求示例：

```json
{
  "taskId": 2001,
  "yoloJson": {
    "sourceName": "demo_surface_001.jpg",
    "weightsName": "demo_model.pt",
    "visualizationName": "demo_surface_001_pred.jpg",
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

核心规则：

- `taskId` 必须存在。
- `CLOSED` / `CANCELLED` 任务不允许导入。
- 检测结果状态默认为 `PENDING_REVIEW`。
- 导入成功后任务状态更新为 `WAIT_REVIEW`。

### GET `/api/detections`

分页查询检测结果。

查询参数：

- `taskId`
- `imageId`
- `className`
- `status`
- `pageNo`
- `pageSize`

### GET `/api/detections/{id}`

查询检测结果详情。

## 5. Review 人工复核

### POST `/api/reviews`

提交人工复核。

请求示例：

```json
{
  "detectionResultId": 4001,
  "reviewerId": 2,
  "reviewResult": "CONFIRMED_DEFECT",
  "reviewComment": "Confirmed defect during manual review."
}
```

允许复核结果：

- `CONFIRMED_DEFECT`
- `FALSE_POSITIVE`
- `NEED_RECHECK`

核心规则：

- 检测结果必须存在。
- 复核人必须存在于 `sys_user`。
- 同一个检测结果只能复核一次。
- 只有 `PENDING_REVIEW` 检测结果可以复核。
- 复核后同步更新 `detection_result.status`。

### GET `/api/reviews`

分页查询复核记录。

查询参数：

- `taskId`
- `imageId`
- `reviewerId`
- `reviewResult`
- `pageNo`
- `pageSize`

### GET `/api/reviews/{id}`

查询复核详情。

### GET `/api/reviews/by-detection/{detectionResultId}`

根据检测结果查询复核记录。

## 6. NCR 不合格记录

### POST `/api/ncrs`

创建 NCR。

请求示例：

```json
{
  "ncrNo": "NCR-DEMO-20260707-001",
  "reviewId": 5001,
  "severity": "HIGH",
  "description": "Confirmed surface defect requires quality tracking.",
  "createdBy": 3
}
```

核心规则：

- `ncrNo` 不能为空且不能重复。
- `reviewId` 必须存在。
- `createdBy` 必须存在于 `sys_user`。
- 只有 `CONFIRMED_DEFECT` 复核记录可以创建 NCR。
- 同一个复核记录只能创建一条 NCR。
- 创建后 NCR 状态默认为 `OPEN`。
- 创建后对应批次状态更新为 `NCR_OPEN`。

### GET `/api/ncrs`

分页查询 NCR。

查询参数：

- `ncrNo`
- `batchId`
- `taskId`
- `severity`
- `status`
- `pageNo`
- `pageSize`

### GET `/api/ncrs/{id}`

查询 NCR 详情。

### GET `/api/ncrs/by-review/{reviewId}`

根据复核记录查询 NCR。

### PATCH `/api/ncrs/{id}/status`

更新 NCR 状态。

允许状态：

- `OPEN`
- `CLOSED`
- `CANCELLED`

`CAPA_CREATED` 由 CAPA 创建流程自动设置，不建议通过 NCR 模块手动设置。

## 7. CAPA 整改闭环

### POST `/api/capas`

创建 CAPA。

请求示例：

```json
{
  "capaNo": "CAPA-DEMO-20260707-001",
  "ncrId": 6001,
  "ownerId": 3,
  "rootCause": "Handling process not stable enough.",
  "correctiveAction": "Recheck affected batch and update handling checklist.",
  "preventiveAction": "Add visual inspection checkpoint before release.",
  "dueDate": "2026-07-15"
}
```

核心规则：

- `capaNo` 不能为空且不能重复。
- `ncrId` 必须存在。
- `ownerId` 必须存在于 `sys_user`。
- 只有 `OPEN` 状态 NCR 可以创建 CAPA。
- 同一个 NCR 只能创建一条 CAPA。
- 创建后 CAPA 状态默认为 `IN_PROGRESS`。
- 创建后 NCR 状态更新为 `CAPA_CREATED`。
- 创建后批次状态更新为 `CAPA_OPEN`。

### GET `/api/capas`

分页查询 CAPA。

查询参数：

- `capaNo`
- `ncrId`
- `batchId`
- `ownerId`
- `status`
- `pageNo`
- `pageSize`

### GET `/api/capas/{id}`

查询 CAPA 详情。

### GET `/api/capas/by-ncr/{ncrId}`

根据 NCR 查询 CAPA。

### PUT `/api/capas/{id}`

更新 CAPA 基础信息。

可更新字段：

- `rootCause`
- `correctiveAction`
- `preventiveAction`
- `verifyResult`
- `dueDate`

`CLOSED` / `CANCELLED` 状态不允许修改基础信息。

### PATCH `/api/capas/{id}/status`

更新 CAPA 状态。

允许状态：

- `PENDING_ANALYSIS`
- `IN_PROGRESS`
- `PENDING_VERIFY`
- `CLOSED`
- `CANCELLED`

当 CAPA 更新为 `CLOSED` 时，会同步关闭 NCR，并将对应批次状态更新为 `CLOSED`。
