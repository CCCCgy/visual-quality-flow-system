package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CapaStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
