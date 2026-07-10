package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/inspection-tasks 的检测任务创建请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * InspectionTaskController#createTask。
 *
 * 下游依赖：
 * InspectionTaskServiceImpl 校验 batchId 指向的批次存在且未关闭，再写入 inspection_task。
 */
@Data
public class InspectionTaskCreateDTO {

    @NotBlank(message = "taskNo cannot be blank")
    private String taskNo;

    @NotNull(message = "batchId cannot be null")
    private Long batchId;

    @NotBlank(message = "modelName cannot be blank")
    private String modelName;

    @NotBlank(message = "modelVersion cannot be blank")
    private String modelVersion;

    private String sourceType;

    @NotNull(message = "createdBy cannot be null")
    private Long createdBy;
}
