package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CapaCreateDTO {

    @NotBlank(message = "capaNo cannot be blank")
    private String capaNo;

    @NotNull(message = "ncrId cannot be null")
    private Long ncrId;

    @NotNull(message = "ownerId cannot be null")
    private Long ownerId;

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private LocalDate dueDate;
}
