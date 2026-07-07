package com.example.visualqms.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CapaRecordVO {

    private Long id;

    private String capaNo;

    private Long ncrId;

    private Long batchId;

    private Long ownerId;

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private String verifyResult;

    private String status;

    private LocalDate dueDate;

    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
