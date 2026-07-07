package com.example.visualqms.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CapaUpdateDTO {

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private String verifyResult;

    private LocalDate dueDate;
}
