package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.ProductionBatchCreateDTO;
import com.example.visualqms.dto.ProductionBatchStatusUpdateDTO;
import com.example.visualqms.dto.ProductionBatchUpdateDTO;
import com.example.visualqms.service.ProductionBatchService;
import com.example.visualqms.vo.ProductionBatchVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供生产批次的 HTTP 接口，包括创建、分页查询、详情、更新和状态调整。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * 前端 batchApi.js 调用查询和详情接口；批次创建/更新接口也可由接口文档或后续页面调用。
 *
 * 下游依赖：
 * 调用 ProductionBatchService，不直接访问 ProductionBatchMapper，避免把唯一性校验、状态限制等业务规则散落到 Web 层。
 *
 * 主要业务链路：
 * BatchListView.vue -> batchApi.js -> GET /api/batches -> ProductionBatchController
 * -> ProductionBatchService -> ProductionBatchServiceImpl -> ProductionBatchMapper -> production_batch。
 *
 * 注意事项：
 * Controller 只负责参数校验和统一响应包装，状态是否合法由 Service 层集中判断。
 */
@Validated
@RestController
@RequestMapping("/api/batches")
public class ProductionBatchController {

    private final ProductionBatchService productionBatchService;

    public ProductionBatchController(ProductionBatchService productionBatchService) {
        this.productionBatchService = productionBatchService;
    }

    /**
     * 创建生产批次。
     *
     * 调用链：
     * 接口调用方
     * -> POST /api/batches
     * -> ProductionBatchController
     * -> ProductionBatchService#createBatch
     * -> ProductionBatchServiceImpl
     * -> ProductionBatchMapper
     * -> production_batch
     *
     * @param dto 前端提交的批次创建参数，batchNo/productCode/productName/plannedQuantity/createdBy 必填
     * @return 创建后的批次信息，外层由 Result 包装
     */
    @PostMapping
    public Result<ProductionBatchVO> createBatch(@Valid @RequestBody ProductionBatchCreateDTO dto) {
        return Result.success(productionBatchService.createBatch(dto));
    }

    /**
     * 分页查询生产批次。
     *
     * 调用链：
     * BatchListView.vue
     * -> batchApi.js#getBatchPage
     * -> GET /api/batches
     * -> ProductionBatchService#pageBatches
     * -> production_batch
     *
     * @param batchNo 批次号模糊查询条件，可为空
     * @param status 批次状态过滤条件，可为空
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 批次分页结果
     */
    @GetMapping
    public Result<PageResult<ProductionBatchVO>> pageBatches(
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(productionBatchService.pageBatches(batchNo, status, pageNo, pageSize));
    }

    /**
     * 查询单个批次详情。
     *
     * 调用链：
     * BatchDetailView.vue
     * -> batchApi.js#getBatchDetail
     * -> GET /api/batches/{id}
     * -> ProductionBatchService#getBatchDetail
     * -> production_batch
     *
     * @param id production_batch 主键
     * @return 批次详情
     */
    @GetMapping("/{id}")
    public Result<ProductionBatchVO> getBatchDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(productionBatchService.getBatchDetail(id));
    }

    /**
     * 更新批次的非状态字段。
     *
     * @param id production_batch 主键
     * @param dto 可更新的产品信息、计划数量和备注
     * @return 更新后的批次信息
     */
    @PutMapping("/{id}")
    public Result<ProductionBatchVO> updateBatch(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody ProductionBatchUpdateDTO dto) {
        return Result.success(productionBatchService.updateBatch(id, dto));
    }

    /**
     * 调整批次状态。
     *
     * @param id production_batch 主键
     * @param dto 目标状态；CLOSED 批次不能再切回非 CLOSED 状态
     * @return 状态更新后的批次信息
     */
    @PatchMapping("/{id}/status")
    public Result<ProductionBatchVO> updateBatchStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody ProductionBatchStatusUpdateDTO dto) {
        return Result.success(productionBatchService.updateBatchStatus(id, dto));
    }
}
