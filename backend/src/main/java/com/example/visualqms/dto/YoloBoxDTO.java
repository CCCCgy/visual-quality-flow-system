package com.example.visualqms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 文件职责：
 * 表达 YOLO JSON 中的单个检测框。
 *
 * 所属层级：
 * DTO。
 *
 * 下游依赖：
 * DetectionImportServiceImpl 将 classId/className/confidence/bboxXyxy 分别写入 detection_result 对应字段。
 */
@Data
public class YoloBoxDTO {

    @JsonAlias("class_id")
    @NotNull(message = "classId cannot be null")
    private Integer classId;

    @JsonAlias("class_name")
    @NotBlank(message = "className cannot be blank")
    private String className;

    @NotNull(message = "confidence cannot be null")
    private BigDecimal confidence;

    @JsonAlias("bbox_xyxy")
    @NotNull(message = "bboxXyxy cannot be null")
    /** 四元素坐标数组：[x1, y1, x2, y2]，导入时拆分为 bbox_x1...bbox_y2。 */
    private List<BigDecimal> bboxXyxy;
}
