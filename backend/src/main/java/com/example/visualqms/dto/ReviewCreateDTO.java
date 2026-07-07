package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateDTO {

    @NotNull(message = "detectionResultId cannot be null")
    private Long detectionResultId;

    @NotNull(message = "reviewerId cannot be null")
    private Long reviewerId;

    @NotBlank(message = "reviewResult cannot be blank")
    private String reviewResult;

    private String reviewComment;
}
