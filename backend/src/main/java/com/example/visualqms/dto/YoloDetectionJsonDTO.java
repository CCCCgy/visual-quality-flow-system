package com.example.visualqms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class YoloDetectionJsonDTO {

    @JsonAlias("source_name")
    @NotBlank(message = "sourceName cannot be blank")
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
    private List<YoloBoxDTO> boxes;
}
