## production_batch 批次管理模块

本模块是系统的业务入口，用于管理一次生产批次的基础信息和质量状态。

核心调用链：

用户请求
→ ProductionBatchController
→ ProductionBatchService
→ ProductionBatchServiceImpl
→ ProductionBatchMapper
→ production_batch 表

核心业务规则：

1. batchNo 不能重复。
2. 创建批次时默认状态为 CREATED。
3. CLOSED 状态的批次不能再修改基础信息。
4. CLOSED 状态的批次不能切换回其他状态。
5. Controller 只负责接收请求和返回结果，业务校验放在 ServiceImpl 中。

该模块验证了 Spring Boot、MyBatis-Plus、MySQL、Knife4j 接口调试的完整后端开发流程。

## inspection_task 检测任务模块

本模块用于管理某个生产批次下的一次视觉检测任务，是 production_batch 和后续 detection_result 的中间层。

核心调用链：

用户请求
→ InspectionTaskController
→ InspectionTaskService
→ InspectionTaskServiceImpl
→ InspectionTaskMapper
→ inspection_task 表

核心业务规则：

1. taskNo 不能重复。
2. 创建检测任务时必须绑定已存在的 production_batch。
3. CLOSED 状态的批次不能继续创建检测任务。
4. 检测任务状态限定为 CREATED、WAIT_REVIEW、REVIEWED、CLOSED、CANCELLED。
5. CLOSED 和 CANCELLED 状态的任务不能切换回其他状态。

该模块体现了业务对象之间的关联校验：检测任务不能脱离生产批次单独存在。


## detection_result 检测结果导入模块

本模块用于将 YOLO 推理输出的 JSON 转换为系统中的业务数据。

核心流程：

YOLO JSON
→ DetectionController
→ DetectionImportService
→ InspectionImageMapper
→ DetectionResultMapper
→ inspection_image / detection_result 表

核心业务规则：

1. 只有存在的检测任务才能导入检测结果。
2. CLOSED / CANCELLED 状态的检测任务不允许继续导入。
3. 导入时根据 sourceName 创建或关联 inspection_image。
4. 每个 box 映射为一条 detection_result。
5. bbox_xyxy 会被拆分为 bbox_x1、bbox_y1、bbox_x2、bbox_y2。
6. 检测结果默认状态为 PENDING_REVIEW。
7. 导入完成后，检测任务状态变为 WAIT_REVIEW。

这个模块体现了 AI 模型输出到业务数据库的转换过程，是本系统与 YOLO 检测项目的衔接点。

## review_record 人工复核模块

本模块用于对模型检测结果进行人工确认，是视觉检测系统进入质量闭环前的重要环节。

核心流程：

detection_result
→ ReviewRecordController
→ ReviewRecordService
→ ReviewRecordMapper
→ review_record 表
→ 同步更新 detection_result.status

核心业务规则：

1. 只有存在的 detection_result 才能复核。
2. 只有 PENDING_REVIEW 状态的检测结果允许复核。
3. 同一个 detection_result 只能生成一条 review_record。
4. reviewerId 必须对应系统中的有效用户。
5. reviewResult 只能是 CONFIRMED_DEFECT、FALSE_POSITIVE、NEED_RECHECK。
6. 提交复核后，需要同步更新 detection_result.status。
7. 复核接口使用事务，保证 review_record 插入和 detection_result 状态更新的一致性。

该模块体现了工业视觉检测场景中的“人机协同”：模型负责初筛，人工负责最终确认，后续 NCR 和 CAPA 只基于复核后的质量结论继续流转。

## ncr_record 不合格记录模块

本模块用于将人工确认后的缺陷转化为质量管理流程中的 NCR 不合格记录。

核心流程：

review_record
→ NcrRecordController
→ NcrRecordService
→ NcrRecordMapper
→ ncr_record 表
→ 更新 production_batch.status 为 NCR_OPEN

核心业务规则：

1. 只有 CONFIRMED_DEFECT 的复核记录才能创建 NCR。
2. FALSE_POSITIVE 和 NEED_RECHECK 不允许创建 NCR。
3. 同一个 review_record 只能创建一条 NCR。
4. 创建 NCR 时，需要从 review_record 追溯 detection_result、inspection_task 和 production_batch。
5. 创建 NCR 后，批次状态更新为 NCR_OPEN。
6. NCR 初始状态为 OPEN。
7. CLOSED / CANCELLED 状态的 NCR 不允许恢复为 OPEN。
8. 创建 NCR 与更新批次状态使用同一个事务，保证数据一致性。
9. 不存在的 reviewId / createdBy 返回 404，非法状态和非法业务流转返回 400。

该模块体现了从“人工确认缺陷”到“质量不合格记录”的业务转换，是质量闭环的入口。

## capa_record CAPA 整改闭环模块

本模块用于将 NCR 不合格记录转化为 CAPA 整改闭环，是质量管理流程的最终闭环环节。

核心流程：

ncr_record
→ CapaRecordController
→ CapaRecordService
→ CapaRecordMapper
→ capa_record 表
→ 更新 ncr_record.status
→ 更新 production_batch.status

核心业务规则：

1. 只有 OPEN 状态的 NCR 才能创建 CAPA。
2. 同一个 NCR 只能创建一条 CAPA。
3. 创建 CAPA 后，CAPA 状态默认为 IN_PROGRESS。
4. 创建 CAPA 后，NCR 状态更新为 CAPA_CREATED。
5. 创建 CAPA 后，批次状态更新为 CAPA_OPEN。
6. CAPA 可以更新根因、纠正措施、预防措施、验证结果和截止日期。
7. CLOSED / CANCELLED 状态的 CAPA 不允许继续修改基础信息。
8. CLOSED / CANCELLED 状态的 CAPA 不允许恢复到其他状态。
9. 关闭 CAPA 时，需要同步关闭 NCR，并将批次状态更新为 CLOSED。
10. 创建 CAPA 与更新 NCR / 批次状态使用同一个事务；关闭 CAPA、关闭 NCR、关闭批次也使用同一个事务。

该模块体现了质量管理中的整改闭环思想：缺陷不是简单记录结束，而是需要追溯原因、制定措施、验证结果，并最终关闭 NCR 与批次质量状态。