package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NcrRecordVO {

    private Long id;

    private String ncrNo;

    private Long batchId;

    private Long taskId;

    private Long detectionResultId;

    private Long reviewId;

    private String severity;

    private String status;

    private String description;

    private Long createdBy;

    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
