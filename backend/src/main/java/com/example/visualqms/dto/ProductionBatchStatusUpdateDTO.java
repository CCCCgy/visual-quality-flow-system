package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductionBatchStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
