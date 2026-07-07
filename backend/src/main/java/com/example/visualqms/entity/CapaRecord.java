package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("capa_record")
public class CapaRecord {

    @TableId(type = IdType.AUTO)
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
