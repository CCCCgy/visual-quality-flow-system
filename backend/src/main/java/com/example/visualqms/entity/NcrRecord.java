package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ncr_record")
public class NcrRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ncrNo;

    private Long batchId;

    private Long taskId;

    private Long detectionResultId;

    private Long reviewId;

    private String severity;

    private String status;

    private String description;

    private Long createdBy;

    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
