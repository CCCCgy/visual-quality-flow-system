package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NcrStatusUpdateDTO {

    @NotBlank(message = "status cannot be blank")
    private String status;
}
