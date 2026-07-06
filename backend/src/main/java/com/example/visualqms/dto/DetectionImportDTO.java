package com.example.visualqms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DetectionImportDTO {

    @NotNull(message = "taskId cannot be null")
    private Long taskId;

    @Valid
    @NotNull(message = "yoloJson cannot be null")
    private YoloDetectionJsonDTO yoloJson;
}
