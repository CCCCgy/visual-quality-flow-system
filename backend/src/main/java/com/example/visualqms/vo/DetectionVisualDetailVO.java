package com.example.visualqms.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回检测结果视觉详情，合并 detection_result 与 inspection_image。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * DetectionResultServiceImpl 先查 detection_result，再查 inspection_image。
 *
 * 下游依赖：
 * DetectionResultDetailView.vue 使用 imageName/imageUri 和 bbox 坐标在图片上叠加红框与标签。
 *
 * 设计说明：
 * 该 VO 不等同于 Entity，因为它包含跨表字段 imageName 和 imageUri。
 */
@Data
public class DetectionVisualDetailVO {

    private Long id;

    private Long taskId;

    private Long imageId;

    private String imageName;

    private String imageUri;

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
