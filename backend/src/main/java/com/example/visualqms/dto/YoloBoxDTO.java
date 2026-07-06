package com.example.visualqms.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

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
    private List<BigDecimal> bboxXyxy;
}
