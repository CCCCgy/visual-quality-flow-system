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