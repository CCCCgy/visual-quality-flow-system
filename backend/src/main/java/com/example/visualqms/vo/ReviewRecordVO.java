package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewRecordVO {

    private Long id;

    private Long detectionResultId;

    private Long taskId;

    private Long imageId;

    private Long reviewerId;

    private String reviewResult;

    private String reviewComment;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
