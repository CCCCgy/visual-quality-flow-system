package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductionBatchVO {

    private Long id;

    private String batchNo;

    private String productCode;

    private String productName;

    private Integer plannedQuantity;

    private String status;

    private Long createdBy;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
