# 数据库设计

## 1. 设计目标

本数据库用于支撑“工业视觉检测结果复核与质量闭环管理系统原型”的 MVP 主链路：

```text
创建批次 -> 创建检测任务 -> 导入 YOLO JSON -> 生成检测结果 -> 人工复核 -> 生成 NCR -> 创建 / 关闭 CAPA -> Dashboard 展示核心统计
```

第一版只保留 8 张核心表：

| 表名 | 作用 |
| --- | --- |
| `sys_user` | 系统用户、复核员、质量工程师等账号信息 |
| `production_batch` | 生产批次，是检测与质量闭环的业务入口 |
| `inspection_task` | 检测任务，表示某个批次的一次视觉检测导入与复核流程 |
| `inspection_image` | 检测图片，记录图片与任务的关系 |
| `detection_result` | 模型检测出的缺陷候选框结果 |
| `review_record` | 人工复核记录，一条检测结果最多一条复核记录 |
| `ncr_record` | 不合格记录 NCR，记录已确认的质量问题 |
| `capa_record` | CAPA 整改记录，记录纠正预防措施与验证关闭情况 |

设计原则：

- 使用 MySQL 8 语法。
- 每张表使用 `bigint` 主键。
- 每张表包含 `created_time`、`updated_time`。
- 关键业务表包含 `status` 字段。
- 不使用真实企业数据。
- 不保存本地绝对路径，不保存真实 YOLO 权重路径。
- 第一版不使用物理外键，只使用逻辑外键字段，例如 `batch_id`、`task_id`、`image_id`、`review_id`。

## 2. 核心关系

```text
sys_user
  |-- production_batch.created_by
  |-- inspection_task.created_by
  |-- review_record.reviewer_id
  |-- ncr_record.created_by
  |-- capa_record.owner_id

production_batch 1 -- N inspection_task
inspection_task 1 -- N inspection_image
inspection_image 1 -- N detection_result
detection_result 1 -- 0/1 review_record
review_record 1 -- 0/1 ncr_record
production_batch 1 -- N ncr_record
inspection_task 1 -- N ncr_record
ncr_record 1 -- 0/1 capa_record
```

说明：

- 一个生产批次可以有多个检测任务，例如首检、复检、补充检测。
- 一个检测任务可以导入多张图片。
- 一张图片可以产生多个模型检测结果。
- 一个检测结果只能对应一条复核记录，通过 `uk_review_detection_result_id` 保证。
- 一个复核记录最多生成一条 NCR，通过 `uk_ncr_review_id` 保证。
- 一个 NCR 最多对应一条 CAPA，通过 `uk_capa_ncr_id` 保证。
- NCR 同时保留 `batch_id`、`task_id`、`detection_result_id`、`review_id`，保证追溯链路完整。

## 3. 状态枚举总览

### 3.1 production_batch.status

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 批次已创建，尚未开始检测 |
| `INSPECTING` | 批次存在进行中的检测任务 |
| `NCR_OPEN` | 批次存在未关闭的不合格记录 |
| `CAPA_OPEN` | 批次存在进行中的 CAPA |
| `CLOSED` | 批次质量流程已关闭 |

### 3.2 inspection_task.status

| 状态 | 含义 |
| --- | --- |
| `CREATED` | 检测任务已创建，尚未导入模型结果 |
| `WAIT_REVIEW` | 已导入 YOLO JSON 并生成检测结果，等待或正在人工复核 |
| `REVIEWED` | 检测结果已完成复核 |
| `CLOSED` | 检测任务已关闭 |
| `CANCELLED` | 检测任务取消 |

### 3.3 inspection_image.status

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 图片已登记，尚未生成检测结果 |
| `DETECTED` | 已生成检测结果 |
| `REVIEWED` | 该图片下检测结果已完成复核 |

### 3.4 detection_result.status

| 状态 | 含义 |
| --- | --- |
| `PENDING_REVIEW` | 待人工复核 |
| `CONFIRMED_DEFECT` | 人工确认是真实缺陷 |
| `FALSE_POSITIVE` | 人工判断为误检 |
| `NEED_RECHECK` | 人工判断需要复检 |

### 3.5 review_record.review_result

| 结果 | 含义 |
| --- | --- |
| `CONFIRMED_DEFECT` | 确认缺陷 |
| `FALSE_POSITIVE` | 标记误检 |
| `NEED_RECHECK` | 需要复检 |

### 3.6 ncr_record.status

| 状态 | 含义 |
| --- | --- |
| `OPEN` | NCR 已创建，待处理 |
| `CAPA_CREATED` | 已创建关联 CAPA |
| `CLOSED` | NCR 已关闭 |
| `CANCELLED` | NCR 作废 |

### 3.7 capa_record.status

| 状态 | 含义 |
| --- | --- |
| `PENDING_ANALYSIS` | CAPA 已创建，待根因分析和措施制定 |
| `IN_PROGRESS` | CAPA 正在执行 |
| `PENDING_VERIFY` | CAPA 措施已完成，等待效果验证 |
| `CLOSED` | CAPA 已验证并关闭 |
| `CANCELLED` | CAPA 作废 |

### 3.8 sys_user.status

| 状态 | 含义 |
| --- | --- |
| `ACTIVE` | 用户启用 |
| `DISABLED` | 用户停用 |

## 4. 表结构说明

### 4.1 sys_user

用户表用于保存系统内演示账号，不保存真实个人敏感信息。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 用户主键 |
| `username` | 登录名或系统内用户名 |
| `display_name` | 页面展示名称 |
| `role_code` | 角色编码，例如 `ADMIN`、`INSPECTOR`、`QUALITY_ENGINEER` |
| `status` | 用户状态，见 `sys_user.status` |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.2 production_batch

生产批次表是主流程入口，用于承载批次编号、产品信息和批次状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 批次主键 |
| `batch_no` | 批次编号，演示数据使用脱敏编号 |
| `product_code` | 产品编码，演示数据使用通用编码 |
| `product_name` | 产品名称，演示数据使用通用名称 |
| `planned_quantity` | 计划数量 |
| `status` | 批次状态 |
| `created_by` | 创建人用户 ID，逻辑关联 `sys_user.id` |
| `remark` | 备注 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.3 inspection_task

检测任务表表示一次视觉检测导入和复核流程。任务状态只描述检测复核阶段，不再承载 NCR 或 CAPA 的状态。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 检测任务主键 |
| `task_no` | 检测任务编号 |
| `batch_id` | 所属批次 ID，逻辑关联 `production_batch.id` |
| `model_name` | 模型名称，只保存通用名称，不保存权重路径 |
| `model_version` | 模型版本 |
| `source_type` | 来源类型，例如 `YOLO_JSON` |
| `status` | 检测任务状态：`CREATED`、`WAIT_REVIEW`、`REVIEWED`、`CLOSED`、`CANCELLED` |
| `created_by` | 创建人用户 ID |
| `imported_time` | 导入模型结果时间 |
| `reviewed_time` | 复核完成时间 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.4 inspection_image

检测图片表保存任务下的图片信息。`image_uri` 仅保存相对路径或对象存储 key，不保存本地绝对路径。`width` 和 `height` 允许为空，适配导入阶段暂时无法读取图片尺寸的情况。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 图片主键 |
| `task_id` | 所属检测任务 ID |
| `batch_id` | 冗余批次 ID，便于查询 |
| `image_name` | 图片名称 |
| `image_uri` | 图片相对路径或资源 key |
| `width` | 图片宽度，允许为空 |
| `height` | 图片高度，允许为空 |
| `status` | 图片状态 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.5 detection_result

检测结果表保存模型输出的缺陷候选框。类别字段采用 YOLO 常见的 `class_id` 和 `class_name` 表达；坐标按像素值保存为左上角和右下角坐标。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 检测结果主键 |
| `task_id` | 所属检测任务 ID |
| `image_id` | 所属图片 ID |
| `class_id` | 模型类别 ID |
| `class_name` | 模型类别名称，例如 `scratch`、`dent`、`stain` |
| `confidence` | 模型置信度 |
| `bbox_x1` | 缺陷框左上角 x 坐标 |
| `bbox_y1` | 缺陷框左上角 y 坐标 |
| `bbox_x2` | 缺陷框右下角 x 坐标 |
| `bbox_y2` | 缺陷框右下角 y 坐标 |
| `status` | 复核状态：`PENDING_REVIEW`、`CONFIRMED_DEFECT`、`FALSE_POSITIVE`、`NEED_RECHECK` |
| `raw_payload` | 原始检测片段 JSON |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.6 review_record

复核记录表保存人工判断。该表不直接替代 `detection_result.status`，而是作为操作留痕；复核后应同步更新检测结果状态。一条 `detection_result` 只能对应一条 `review_record`。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 复核记录主键 |
| `detection_result_id` | 检测结果 ID，唯一约束 `uk_review_detection_result_id` |
| `task_id` | 检测任务 ID |
| `image_id` | 图片 ID |
| `reviewer_id` | 复核人用户 ID |
| `review_result` | 复核结果：`CONFIRMED_DEFECT`、`FALSE_POSITIVE`、`NEED_RECHECK` |
| `review_comment` | 复核说明 |
| `reviewed_time` | 复核时间 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.7 ncr_record

NCR 表用于记录确认后的不合格问题。MVP 中通常由复核结果为 `CONFIRMED_DEFECT` 的记录触发创建。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | NCR 主键 |
| `ncr_no` | NCR 编号 |
| `batch_id` | 所属批次 ID |
| `task_id` | 来源检测任务 ID |
| `detection_result_id` | 来源检测结果 ID |
| `review_id` | 来源复核记录 ID，唯一约束 `uk_ncr_review_id` |
| `severity` | 严重程度，例如 `LOW`、`MEDIUM`、`HIGH` |
| `status` | NCR 状态 |
| `description` | 问题描述 |
| `created_by` | 创建人用户 ID |
| `closed_time` | 关闭时间 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

### 4.8 capa_record

CAPA 表用于记录纠正预防措施，从 NCR 开始，到根因分析、措施执行、效果验证和关闭结束。一个 NCR 最多创建一条 CAPA。

关键字段：

| 字段 | 说明 |
| --- | --- |
| `id` | CAPA 主键 |
| `capa_no` | CAPA 编号 |
| `ncr_id` | 来源 NCR ID，唯一约束 `uk_capa_ncr_id` |
| `batch_id` | 所属批次 ID |
| `owner_id` | 负责人用户 ID |
| `root_cause` | 根因分析 |
| `corrective_action` | 纠正措施，允许为空 |
| `preventive_action` | 预防措施 |
| `verify_result` | 验证结果 |
| `status` | CAPA 状态：`PENDING_ANALYSIS`、`IN_PROGRESS`、`PENDING_VERIFY`、`CLOSED`、`CANCELLED` |
| `due_date` | 计划完成日期 |
| `closed_time` | 关闭时间 |
| `created_time` | 创建时间 |
| `updated_time` | 更新时间 |

## 5. Dashboard 统计口径

MVP Dashboard 可以基于上述 8 张表统计：

| 指标 | 来源表 | 口径 |
| --- | --- | --- |
| 批次数量 | `production_batch` | 按 `status` 分组统计 |
| 检测任务数量 | `inspection_task` | 按 `status` 分组统计 |
| 图片数量 | `inspection_image` | 统计任务下图片数 |
| 模型缺陷候选数 | `detection_result` | 统计所有检测框 |
| 确认缺陷数 | `detection_result` | `status = 'CONFIRMED_DEFECT'` |
| 误检数 | `detection_result` | `status = 'FALSE_POSITIVE'` |
| 待复核数 | `detection_result` | `status = 'PENDING_REVIEW'` |
| NCR 数量 | `ncr_record` | 按 `status` 分组统计 |
| CAPA 数量 | `capa_record` | 按 `status` 分组统计 |

## 6. 非目标

以下内容不进入 Day 2 数据库设计：

- MES、ERP、QMS 的完整主数据建模。
- 真实模型训练记录和权重文件管理。
- 真实图片文件上传存储方案。
- 多租户、权限菜单、数据权限。
- 复杂审批流和电子签名。
- 物理外键、触发器、存储过程。
