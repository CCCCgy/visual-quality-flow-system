package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PATCH /api/inspection-tasks/{id}/status 的任务状态更新请求。
 *
 * 所属层级：
 * DTO。
 *
 * 状态约束：
 * CLOSED/CANCELLED 终态不可逆，由 InspectionTaskServiceImpl 校验。
 */
@Data
public class InspectionTaskStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
