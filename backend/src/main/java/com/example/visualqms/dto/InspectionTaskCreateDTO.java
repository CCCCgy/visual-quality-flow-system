package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
