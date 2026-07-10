package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ProductionBatchCreateDTO;
import com.example.visualqms.dto.ProductionBatchStatusUpdateDTO;
import com.example.visualqms.dto.ProductionBatchUpdateDTO;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.vo.ProductionBatchVO;

/**
 * 文件职责：
 * 定义生产批次模块提供给 Controller 的业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * ProductionBatchController。
 *
 * 下游依赖：
 * 由 ProductionBatchServiceImpl 实现，并通过 ProductionBatchMapper 访问 production_batch。
 *
 * 设计说明：
 * Service 接口隔离 Web 层和持久层，Controller 不需要知道唯一性校验、状态限制和 Entity/VO 转换细节。
 */
public interface ProductionBatchService extends IService<ProductionBatch> {

    /** 创建批次，业务动作对应 POST /api/batches。 */
    ProductionBatchVO createBatch(ProductionBatchCreateDTO dto);

    /** 分页查询批次，业务动作对应 GET /api/batches。 */
    PageResult<ProductionBatchVO> pageBatches(String batchNo, String status, Long pageNo, Long pageSize);

    /** 查询批次详情，业务动作对应 GET /api/batches/{id}。 */
    ProductionBatchVO getBatchDetail(Long id);

    /** 更新批次基础信息，业务动作对应 PUT /api/batches/{id}。 */
    ProductionBatchVO updateBatch(Long id, ProductionBatchUpdateDTO dto);

    /** 更新批次状态，业务动作对应 PATCH /api/batches/{id}/status。 */
    ProductionBatchVO updateBatchStatus(Long id, ProductionBatchStatusUpdateDTO dto);
}
