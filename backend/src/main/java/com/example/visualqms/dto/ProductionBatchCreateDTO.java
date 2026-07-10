package com.example.visualqms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/batches 的批次创建请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * 接口调用方提交 JSON，请求进入 ProductionBatchController#createBatch。
 *
 * 下游依赖：
 * ProductionBatchServiceImpl 将本对象转换为 ProductionBatch，但不会让 DTO 直接替代 Entity。
 *
 * 注意事项：
 * DTO 表达接口输入约束；Entity 表达数据库表结构，二者职责不同。
 */
@Data
public class ProductionBatchCreateDTO {

    @NotBlank(message = "batchNo cannot be blank")
    private String batchNo;

    @NotBlank(message = "productCode cannot be blank")
    private String productCode;

    @NotBlank(message = "productName cannot be blank")
    private String productName;

    @NotNull(message = "plannedQuantity cannot be null")
    @Min(value = 0, message = "plannedQuantity cannot be less than 0")
    private Integer plannedQuantity;

    @NotNull(message = "createdBy cannot be null")
    private Long createdBy;

    private String remark;
}
