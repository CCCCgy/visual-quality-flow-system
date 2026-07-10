package com.example.visualqms.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PUT /api/batches/{id} 的批次基础信息更新请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * ProductionBatchController#updateBatch。
 *
 * 下游依赖：
 * ProductionBatchServiceImpl 只更新非 null 字段，并禁止 CLOSED 批次修改。
 */
@Data
public class ProductionBatchUpdateDTO {

    private String productCode;

    private String productName;

    @Min(value = 0, message = "plannedQuantity cannot be less than 0")
    private Integer plannedQuantity;

    private String remark;
}
