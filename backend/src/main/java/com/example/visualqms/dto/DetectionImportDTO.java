package com.example.visualqms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/detections/import-json 的 YOLO 导入请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * DetectionController#importYoloJson。
 *
 * 下游依赖：
 * DetectionImportServiceImpl 根据 taskId 找到 inspection_task，并将 yoloJson 转为 inspection_image 和 detection_result。
 */
@Data
public class DetectionImportDTO {

    @NotNull(message = "taskId cannot be null")
    private Long taskId;

    @Valid
    @NotNull(message = "yoloJson cannot be null")
    private YoloDetectionJsonDTO yoloJson;
}
