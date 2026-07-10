package com.example.visualqms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 文件职责：
 * 表达一次 YOLO JSON 的顶层结构。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * DetectionImportDTO.yoloJson 嵌套传入。
 *
 * 下游依赖：
 * DetectionImportServiceImpl 使用 sourceName 定位 inspection_image，使用 boxes 创建 detection_result。
 *
 * 注意事项：
 * JsonAlias 同时兼容 snake_case 示例数据和 Java camelCase 请求字段。
 */
@Data
public class YoloDetectionJsonDTO {

    @JsonAlias("source_name")
    @NotBlank(message = "sourceName cannot be blank")
    /** 原始图片文件名，导入时对应 inspection_image.image_name。 */
    private String sourceName;

    @JsonAlias("weights_name")
    private String weightsName;

    @JsonAlias("visualization_name")
    private String visualizationName;

    @JsonAlias("class_names")
    private List<String> classNames;

    private Map<String, Object> parameters;

    @JsonAlias("inference_time_ms")
    private Long inferenceTimeMs;

    @Valid
    @NotNull(message = "boxes cannot be null")
    /** YOLO 检测框列表，每个 box 会落为一条 detection_result。 */
    private List<YoloBoxDTO> boxes;
}
