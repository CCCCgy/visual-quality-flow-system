package com.example.visualqms.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DetectionResultVO {

    private Long id;

    private Long taskId;

    private Long imageId;

    private Integer classId;

    private String className;

    private BigDecimal confidence;

    private BigDecimal bboxX1;

    private BigDecimal bboxY1;

    private BigDecimal bboxX2;

    private BigDecimal bboxY2;

    private String status;

    private String rawPayload;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
