# 状态机设计

## 1. 目标

本文冻结 Day 2 MVP 的业务状态机，确保文档、schema SQL 和后续 API 实现使用同一套状态枚举。

主链路：

```text
创建批次
-> 创建检测任务
-> 导入 YOLO JSON
-> 生成检测结果
-> 人工复核
-> 生成 NCR
-> 创建 / 关闭 CAPA
-> Dashboard 展示核心统计
```

状态机设计重点：

- 状态必须能覆盖演示主流程。
- 状态数量保持克制，避免第一版过度设计。
- 状态流转由应用服务控制，数据库只保存当前状态。
- `inspection_task.status` 只描述检测复核阶段，不承载 NCR 或 CAPA 状态。
- `review_record` 是复核留痕，`detection_result.status` 是当前复核结论。

## 2. 批次状态机

### 2.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 批次已创建，尚未开始检测 |
| `INSPECTING` | 批次存在进行中的检测任务 |
| `NCR_OPEN` | 批次存在未关闭的不合格记录 |
| `CAPA_OPEN` | 批次存在进行中的 CAPA |
| `CLOSED` | 批次质量流程已关闭 |

### 2.2 合法流转

```text
CREATED -> INSPECTING
INSPECTING -> NCR_OPEN
INSPECTING -> CLOSED
NCR_OPEN -> CAPA_OPEN
NCR_OPEN -> CLOSED
CAPA_OPEN -> CLOSED
```

说明：

- 如果检测任务完成后没有确认缺陷，批次可以从 `INSPECTING` 直接进入 `CLOSED`。
- 如果存在确认缺陷并生成 NCR，批次进入 `NCR_OPEN`。
- 如果 NCR 创建 CAPA，批次进入 `CAPA_OPEN`。
- CAPA 关闭并验证后，批次进入 `CLOSED`。

### 2.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CREATED -> NCR_OPEN` | 尚未创建检测任务和确认缺陷，不能直接产生 NCR |
| `CREATED -> CAPA_OPEN` | CAPA 必须来源于 NCR |
| `CLOSED -> INSPECTING` | 已关闭批次不应重新进入检测流程，需创建新任务或新批次 |
| `CAPA_OPEN -> NCR_OPEN` | CAPA 已进入整改环节，不应回退 NCR 状态 |

## 3. 检测任务状态机

### 3.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 检测任务已创建，尚未导入模型结果 |
| `WAIT_REVIEW` | 已导入 YOLO JSON 并生成检测结果，等待或正在人工复核 |
| `REVIEWED` | 检测结果已完成复核 |
| `CLOSED` | 检测任务已关闭 |
| `CANCELLED` | 检测任务取消 |

### 3.2 合法流转

```text
CREATED -> WAIT_REVIEW
CREATED -> CANCELLED
WAIT_REVIEW -> REVIEWED
WAIT_REVIEW -> CANCELLED
REVIEWED -> CLOSED
```

说明：

- `CREATED` 表示任务壳已存在，但还没有导入模型结果。
- 导入 YOLO JSON 并生成图片与检测结果后，任务进入 `WAIT_REVIEW`。
- 第一版不再区分“等待复核”和“复核中”，统一使用 `WAIT_REVIEW`。
- 所有检测结果完成复核后进入 `REVIEWED`。
- NCR 和 CAPA 使用各自表的状态表达，不再写入 `inspection_task.status`。

### 3.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CREATED -> REVIEWED` | 未导入模型结果，无法直接完成复核 |
| `WAIT_REVIEW -> CLOSED` | 复核未完成，不能关闭任务 |
| `REVIEWED -> WAIT_REVIEW` | 已完成复核后不应无痕回退 |
| `CANCELLED -> WAIT_REVIEW` | 已取消任务不应继续导入或复核 |
| `CLOSED -> WAIT_REVIEW` | 已关闭任务不应重新复核 |
| `REVIEWED -> CAPA_OPEN` | `CAPA_OPEN` 不再是检测任务状态 |

## 4. 图片状态机

### 4.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 图片已登记，尚未生成检测结果 |
| `DETECTED` | 已生成检测结果 |
| `REVIEWED` | 该图片下检测结果已完成复核 |

### 4.2 合法流转

```text
PENDING -> DETECTED
DETECTED -> REVIEWED
```

### 4.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `PENDING -> REVIEWED` | 图片尚未产生检测结果 |
| `REVIEWED -> DETECTED` | 已复核图片不应回退到仅检测状态 |

## 5. 检测结果状态机

### 5.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `PENDING_REVIEW` | 待人工复核 |
| `CONFIRMED_DEFECT` | 人工确认是真实缺陷 |
| `FALSE_POSITIVE` | 人工判断为误检 |
| `NEED_RECHECK` | 人工判断需要复检 |

### 5.2 合法流转

```text
PENDING_REVIEW -> CONFIRMED_DEFECT
PENDING_REVIEW -> FALSE_POSITIVE
PENDING_REVIEW -> NEED_RECHECK
NEED_RECHECK -> PENDING_REVIEW
NEED_RECHECK -> CONFIRMED_DEFECT
NEED_RECHECK -> FALSE_POSITIVE
```

说明：

- 模型导入后的检测结果默认是 `PENDING_REVIEW`。
- 人工复核可以确认缺陷、标记误检或要求复检。
- `NEED_RECHECK` 表示当前结果不能直接作为最终质量结论。
- 复检完成后可重新进入 `PENDING_REVIEW`，也可直接给出最终复核结论。

### 5.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `PENDING_REVIEW -> OPEN` | `OPEN` 是 NCR 状态，不是检测结果状态 |
| `FALSE_POSITIVE -> CONFIRMED_DEFECT` | 已定为误检后不应直接改为缺陷，应按业务规则重新打开并保留原因 |
| `CONFIRMED_DEFECT -> PENDING_REVIEW` | 已确认缺陷不应无痕回退，需保留复核记录和变更原因 |

## 6. NCR 状态机

### 6.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `OPEN` | NCR 已创建，待处理 |
| `CAPA_CREATED` | 已创建关联 CAPA |
| `CLOSED` | NCR 已关闭 |
| `CANCELLED` | NCR 作废 |

### 6.2 合法流转

```text
OPEN -> CAPA_CREATED
OPEN -> CLOSED
OPEN -> CANCELLED
CAPA_CREATED -> CLOSED
CAPA_CREATED -> CANCELLED
```

说明：

- 如果问题轻微，NCR 可以不创建 CAPA，直接处理后关闭。
- 如果需要整改闭环，先创建 CAPA，再进入 `CAPA_CREATED`。
- `CANCELLED` 用于演示误建单据或无效单据。

### 6.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `OPEN -> CAPA_OPEN` | `CAPA_OPEN` 是批次状态，不是 NCR 状态 |
| `CLOSED -> CAPA_CREATED` | NCR 已关闭，不能再创建 CAPA |
| `CANCELLED -> CLOSED` | 作废单据不应再按正常关闭处理 |

## 7. CAPA 状态机

### 7.1 状态定义

| 状态 | 含义 |
| --- | --- |
| `PENDING_ANALYSIS` | CAPA 已创建，待根因分析和措施制定 |
| `IN_PROGRESS` | CAPA 正在执行 |
| `PENDING_VERIFY` | CAPA 措施已完成，等待效果验证 |
| `CLOSED` | CAPA 已验证并关闭 |
| `CANCELLED` | CAPA 作废 |

### 7.2 合法流转

```text
PENDING_ANALYSIS -> IN_PROGRESS
PENDING_ANALYSIS -> CANCELLED
IN_PROGRESS -> PENDING_VERIFY
IN_PROGRESS -> CANCELLED
PENDING_VERIFY -> CLOSED
PENDING_VERIFY -> IN_PROGRESS
PENDING_VERIFY -> CANCELLED
```

说明：

- CAPA 创建后默认是 `PENDING_ANALYSIS`。
- 完成根因分析和措施制定后进入 `IN_PROGRESS`。
- 措施执行完成后进入 `PENDING_VERIFY`。
- 验证通过后进入 `CLOSED`。
- 如果验证不通过，可以从 `PENDING_VERIFY` 回到 `IN_PROGRESS` 继续整改。

### 7.3 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `PENDING_ANALYSIS -> CLOSED` | 未执行措施且未验证，不能直接关闭 |
| `IN_PROGRESS -> CLOSED` | 未经过验证环节，不能直接关闭 |
| `CLOSED -> IN_PROGRESS` | 已关闭 CAPA 不应重新执行 |
| `CANCELLED -> IN_PROGRESS` | 已作废 CAPA 不应再执行 |
| `OPEN -> IN_PROGRESS` | `OPEN` 不再是 CAPA 状态 |

## 8. 复核结果与检测结果的映射

`review_record.review_result` 与 `detection_result.status` 使用同一套结果枚举：

| review_result | detection_result.status |
| --- | --- |
| `CONFIRMED_DEFECT` | `CONFIRMED_DEFECT` |
| `FALSE_POSITIVE` | `FALSE_POSITIVE` |
| `NEED_RECHECK` | `NEED_RECHECK` |

复核动作应同时完成两件事：

1. 新增一条 `review_record`。
2. 更新对应 `detection_result.status`。

一条 `detection_result` 只能产生一条 `review_record`。如需重新复核，第一版建议更新原复核记录并保留变更说明；后续版本再扩展复核历史表。

## 9. MVP 演示路径

推荐演示路径：

```text
production_batch.CREATED
-> production_batch.INSPECTING

inspection_task.CREATED
-> inspection_task.WAIT_REVIEW
-> inspection_task.REVIEWED

detection_result.PENDING_REVIEW
-> detection_result.CONFIRMED_DEFECT

review_record.CONFIRMED_DEFECT
-> ncr_record.OPEN
-> ncr_record.CAPA_CREATED

capa_record.PENDING_ANALYSIS
-> capa_record.IN_PROGRESS
-> capa_record.PENDING_VERIFY
-> capa_record.CLOSED

ncr_record.CAPA_CREATED
-> ncr_record.CLOSED

production_batch.NCR_OPEN
-> production_batch.CAPA_OPEN
-> production_batch.CLOSED

inspection_task.REVIEWED
-> inspection_task.CLOSED
```

Dashboard 不改变状态，只读取当前表数据进行统计展示。
