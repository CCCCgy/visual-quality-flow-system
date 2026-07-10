package com.example.visualqms.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回给检测结果列表和基础详情接口的检测结果视图对象。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * detection_result 单表。
 *
 * 下游依赖：
 * DetectionResultListView.vue 使用类别、置信度、bbox 和 status 展示列表并判断是否允许人工复核。
 */
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
