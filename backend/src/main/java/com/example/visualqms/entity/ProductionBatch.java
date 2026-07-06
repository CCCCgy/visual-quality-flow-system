package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("production_batch")
public class ProductionBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String productCode;

    private String productName;

    private Integer plannedQuantity;

    private String status;

    private Long createdBy;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
