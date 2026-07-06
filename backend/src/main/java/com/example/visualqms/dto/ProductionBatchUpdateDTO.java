package com.example.visualqms.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductionBatchUpdateDTO {

    private String productCode;

    private String productName;

    @Min(value = 0, message = "plannedQuantity cannot be less than 0")
    private Integer plannedQuantity;

    private String remark;
}
