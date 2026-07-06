package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inspection_task")
public class InspectionTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long batchId;

    private String modelName;

    private String modelVersion;

    private String sourceType;

    private String status;

    private Long createdBy;

    private LocalDateTime importedTime;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
