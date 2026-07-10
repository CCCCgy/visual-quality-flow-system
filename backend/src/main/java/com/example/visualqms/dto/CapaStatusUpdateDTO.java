package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PATCH /api/capas/{id}/status 的 CAPA 状态更新请求。
 *
 * 所属层级：
 * DTO。
 *
 * 状态约束：
 * CapaRecordServiceImpl 在目标为 CLOSED 时会同步关闭 NCR 和批次。
 */
@Data
public class CapaStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
