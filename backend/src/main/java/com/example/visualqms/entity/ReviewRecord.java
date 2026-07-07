package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_record")
public class ReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long detectionResultId;

    private Long taskId;

    private Long imageId;

    private Long reviewerId;

    private String reviewResult;

    private String reviewComment;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
