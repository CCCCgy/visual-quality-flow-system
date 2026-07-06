package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InspectionTaskVO {

    private Long id;

    private String taskNo;

    private Long batchId;

    private String modelName;

    private String modelVersion;

    private String sourceType;

    private String status;

    private Long createdBy;

    private LocalDateTime importedTime;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
