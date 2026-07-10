package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PATCH /api/batches/{id}/status 的批次状态更新请求。
 *
 * 所属层级：
 * DTO。
 *
 * 状态约束：
 * 目标状态合法性和 CLOSED 不可逆规则由 ProductionBatchServiceImpl 校验。
 */
@Data
public class ProductionBatchStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
