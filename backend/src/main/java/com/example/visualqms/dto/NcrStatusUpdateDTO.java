package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PATCH /api/ncrs/{id}/status 的 NCR 状态更新请求。
 *
 * 所属层级：
 * DTO。
 *
 * 状态约束：
 * 人工接口只允许 OPEN/CLOSED/CANCELLED，CAPA_CREATED 由创建 CAPA 自动产生。
 */
@Data
public class NcrStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
