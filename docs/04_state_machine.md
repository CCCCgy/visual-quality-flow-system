# 状态机设计

## 1. 目标

本文整理当前后端主链路使用的业务状态机，确保文档、数据库枚举和 API 行为保持一致。

主链路：

```text
生产批次
-> 检测任务
-> YOLO JSON 导入
-> 检测结果
-> 人工复核
-> NCR 不合格记录
-> CAPA 整改闭环
```

状态机设计原则：

- 状态只表达当前业务阶段，不记录完整历史。
- 历史动作通过 `review_record`、`ncr_record`、`capa_record` 等业务表留痕。
- `CLOSED` / `CANCELLED` 一般视为终态，不允许恢复到处理中状态。
- 跨表状态变化由 Service 层事务控制，数据库不使用物理外键强制流转。

## 2. production_batch 批次状态

### 状态定义

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 批次已创建，尚未进入检测流程 |
| `INSPECTING` | 批次正在进行视觉检测或结果导入 |
| `NCR_OPEN` | 批次存在已确认缺陷并创建了 OPEN NCR |
| `CAPA_OPEN` | 批次存在进行中的 CAPA |
| `CLOSED` | 批次质量流程已关闭 |

### 推荐流转

```text
CREATED -> INSPECTING -> NCR_OPEN -> CAPA_OPEN -> CLOSED
```

说明：

- 创建批次后默认为 `CREATED`。
- 检测任务开始或导入检测结果后，可进入 `INSPECTING`。
- 创建 NCR 后，系统将批次状态更新为 `NCR_OPEN`。
- 创建 CAPA 后，系统将批次状态更新为 `CAPA_OPEN`。
- 关闭 CAPA 后，系统同步将批次状态更新为 `CLOSED`。

### 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CREATED -> CAPA_OPEN` | CAPA 必须来源于 NCR，不能跳过检测、复核和 NCR |
| `NCR_OPEN -> CREATED` | 已出现质量问题，不应回退为刚创建状态 |
| `CAPA_OPEN -> INSPECTING` | 已进入整改闭环，不应无痕回退到检测阶段 |
| `CLOSED -> INSPECTING` | 已关闭批次不应恢复检测流程 |

## 3. inspection_task 检测任务状态

### 状态定义

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 检测任务已创建，尚未导入模型结果 |
| `WAIT_REVIEW` | 已导入 YOLO JSON 并生成检测结果，等待人工复核 |
| `REVIEWED` | 检测结果已完成复核 |
| `CLOSED` | 检测任务已关闭 |
| `CANCELLED` | 检测任务已取消 |

### 推荐流转

```text
CREATED -> WAIT_REVIEW -> REVIEWED -> CLOSED
CREATED -> CANCELLED
WAIT_REVIEW -> CANCELLED
```

说明：

- 创建检测任务后默认为 `CREATED`。
- YOLO JSON 导入成功后，任务状态更新为 `WAIT_REVIEW`。
- 第一版不把 NCR / CAPA 状态写入 `inspection_task.status`。
- `CLOSED` / `CANCELLED` 后不允许恢复。

### 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CREATED -> REVIEWED` | 尚未导入检测结果，不能直接完成复核 |
| `WAIT_REVIEW -> CREATED` | 已导入结果，不应回退为空任务 |
| `CLOSED -> WAIT_REVIEW` | 已关闭任务不应重新复核 |
| `CANCELLED -> REVIEWED` | 已取消任务不应继续流转 |

## 4. detection_result 检测结果状态

### 状态定义

| 状态 | 含义 |
| --- | --- |
| `PENDING_REVIEW` | 待人工复核 |
| `CONFIRMED_DEFECT` | 人工确认是真实缺陷 |
| `FALSE_POSITIVE` | 人工判断为误检 |
| `NEED_RECHECK` | 人工判断需要复检 |

### 合法流转

```text
PENDING_REVIEW -> CONFIRMED_DEFECT
PENDING_REVIEW -> FALSE_POSITIVE
PENDING_REVIEW -> NEED_RECHECK
```

说明：

- YOLO JSON 导入后，检测结果默认是 `PENDING_REVIEW`。
- 提交人工复核后，`review_record.review_result` 会同步写入 `detection_result.status`。
- 一条检测结果第一版只允许提交一条复核记录。

### 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CONFIRMED_DEFECT -> FALSE_POSITIVE` | 已复核结论不应无痕覆盖 |
| `FALSE_POSITIVE -> CONFIRMED_DEFECT` | 误检结果不应直接改成缺陷，应重新建立可追溯流程 |
| `PENDING_REVIEW -> OPEN` | `OPEN` 是 NCR 状态，不是检测结果状态 |

## 5. review_record 复核结果

`review_record` 本身不维护独立状态机，它记录一次人工复核动作。

允许的 `review_result`：

- `CONFIRMED_DEFECT`
- `FALSE_POSITIVE`
- `NEED_RECHECK`

复核动作会同时完成：

1. 新增一条 `review_record`。
2. 更新对应 `detection_result.status`。

只有 `CONFIRMED_DEFECT` 的复核记录可以继续创建 NCR。

## 6. ncr_record 不合格记录状态

### 状态定义

| 状态 | 含义 |
| --- | --- |
| `OPEN` | NCR 已创建，待处理或待创建 CAPA |
| `CAPA_CREATED` | 已基于该 NCR 创建 CAPA |
| `CLOSED` | NCR 已关闭 |
| `CANCELLED` | NCR 已取消或作废 |

### 合法流转

```text
OPEN -> CAPA_CREATED -> CLOSED
OPEN -> CANCELLED
```

说明：

- 创建 NCR 后默认为 `OPEN`。
- 创建 CAPA 后，NCR 自动进入 `CAPA_CREATED`。
- 关闭 CAPA 时，系统同步将 NCR 更新为 `CLOSED`。
- `CLOSED` / `CANCELLED` 不允许恢复为 `OPEN`。

### 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `OPEN -> CAPA_OPEN` | `CAPA_OPEN` 是批次状态，不是 NCR 状态 |
| `CLOSED -> CAPA_CREATED` | 已关闭 NCR 不能再创建 CAPA |
| `CANCELLED -> OPEN` | 已取消 NCR 不应恢复 |

## 7. capa_record 整改闭环状态

### 状态定义

| 状态 | 含义 |
| --- | --- |
| `PENDING_ANALYSIS` | 预留状态，表示待根因分析 |
| `IN_PROGRESS` | CAPA 正在执行 |
| `PENDING_VERIFY` | CAPA 措施已完成，等待效果验证 |
| `CLOSED` | CAPA 已验证并关闭 |
| `CANCELLED` | CAPA 已取消或作废 |

### 合法流转

```text
IN_PROGRESS -> PENDING_VERIFY -> CLOSED
IN_PROGRESS -> CANCELLED
```

说明：

- 当前后端创建 CAPA 后默认进入 `IN_PROGRESS`。
- `PENDING_ANALYSIS` 是保留枚举，可用于后续更细的根因分析流程。
- CAPA 更新为 `CLOSED` 时，会同步关闭对应 NCR，并将批次状态更新为 `CLOSED`。
- `CLOSED` / `CANCELLED` 不允许恢复为处理中状态，也不允许修改基础信息。

### 非法流转示例

| 非法流转 | 原因 |
| --- | --- |
| `CLOSED -> IN_PROGRESS` | 已关闭 CAPA 不应重新执行 |
| `CANCELLED -> IN_PROGRESS` | 已取消 CAPA 不应恢复执行 |
| `IN_PROGRESS -> OPEN` | `OPEN` 不是 CAPA 状态 |

## 8. 主链路状态观察点

推荐演示观察路径：

```text
production_batch.CREATED
-> production_batch.INSPECTING
-> production_batch.NCR_OPEN
-> production_batch.CAPA_OPEN
-> production_batch.CLOSED

inspection_task.CREATED
-> inspection_task.WAIT_REVIEW

detection_result.PENDING_REVIEW
-> detection_result.CONFIRMED_DEFECT

ncr_record.OPEN
-> ncr_record.CAPA_CREATED
-> ncr_record.CLOSED

capa_record.IN_PROGRESS
-> capa_record.PENDING_VERIFY
-> capa_record.CLOSED
```

Dashboard 或接口查询只读取当前状态，不直接改变状态。
