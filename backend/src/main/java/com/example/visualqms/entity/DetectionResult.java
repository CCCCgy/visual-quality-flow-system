package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("detection_result")
public class DetectionResult {

    @TableId(type = IdType.AUTO)
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
