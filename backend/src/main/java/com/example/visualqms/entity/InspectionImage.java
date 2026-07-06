package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inspection_image")
public class InspectionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long batchId;

    private String imageName;

    private String imageUri;

    private Integer width;

    private Integer height;

    private String status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
