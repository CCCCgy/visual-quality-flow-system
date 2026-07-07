package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NcrCreateDTO {

    @NotBlank(message = "ncrNo cannot be blank")
    private String ncrNo;

    @NotNull(message = "reviewId cannot be null")
    private Long reviewId;

    @NotBlank(message = "severity cannot be blank")
    private String severity;

    @NotBlank(message = "description cannot be blank")
    private String description;

    @NotNull(message = "createdBy cannot be null")
    private Long createdBy;
}
