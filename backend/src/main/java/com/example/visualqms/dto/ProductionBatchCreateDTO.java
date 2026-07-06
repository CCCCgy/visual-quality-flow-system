package com.example.visualqms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductionBatchCreateDTO {

    @NotBlank(message = "batchNo cannot be blank")
    private String batchNo;

    @NotBlank(message = "productCode cannot be blank")
    private String productCode;

    @NotBlank(message = "productName cannot be blank")
    private String productName;

    @NotNull(message = "plannedQuantity cannot be null")
    @Min(value = 0, message = "plannedQuantity cannot be less than 0")
    private Integer plannedQuantity;

    @NotNull(message = "createdBy cannot be null")
    private Long createdBy;

    private String remark;
}
