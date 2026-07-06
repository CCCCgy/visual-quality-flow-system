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