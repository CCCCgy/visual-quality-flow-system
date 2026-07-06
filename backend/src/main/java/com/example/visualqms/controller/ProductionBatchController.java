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

@Validated
@RestController
@RequestMapping("/api/batches")
public class ProductionBatchController {

    private final ProductionBatchService productionBatchService;

    public ProductionBatchController(ProductionBatchService productionBatchService) {
        this.productionBatchService = productionBatchService;
    }

    @PostMapping
    public Result<ProductionBatchVO> createBatch(@Valid @RequestBody ProductionBatchCreateDTO dto) {
        return Result.success(productionBatchService.createBatch(dto));
    }

    @GetMapping
    public Result<PageResult<ProductionBatchVO>> pageBatches(
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(productionBatchService.pageBatches(batchNo, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ProductionBatchVO> getBatchDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(productionBatchService.getBatchDetail(id));
    }

    @PutMapping("/{id}")
    public Result<ProductionBatchVO> updateBatch(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody ProductionBatchUpdateDTO dto) {
        return Result.success(productionBatchService.updateBatch(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<ProductionBatchVO> updateBatchStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody ProductionBatchStatusUpdateDTO dto) {
        return Result.success(productionBatchService.updateBatchStatus(id, dto));
    }
}
