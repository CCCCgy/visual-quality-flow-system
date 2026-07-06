package com.example.visualqms.vo;

import lombok.Data;

@Data
public class DetectionImportResultVO {

    private Long taskId;

    private Long imageId;

    private Integer imageCount;

    private Integer detectionCount;
}
