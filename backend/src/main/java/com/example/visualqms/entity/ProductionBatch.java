package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 production_batch 生产批次表，是质量闭环的起点。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * ProductionBatchServiceImpl 直接管理本表；NcrRecordServiceImpl 和 CapaRecordServiceImpl 会同步更新批次状态。
 *
 * 下游依赖：
 * ProductionBatchMapper 基于该实体执行 MyBatis-Plus CRUD。
 *
 * 表关系：
 * id 被 inspection_task.batch_id、inspection_image.batch_id、ncr_record.batch_id、capa_record.batch_id 逻辑引用。
 *
 * 状态字段：
 * status 表示批次所处质量阶段：CREATED/INSPECTING/NCR_OPEN/CAPA_OPEN/CLOSED。
 */
@Data
@TableName("production_batch")
public class ProductionBatch {

    /** production_batch 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次业务编号，数据库唯一，用于创建批次时防重。 */
    private String batchNo;

    private String productCode;

    private String productName;

    private Integer plannedQuantity;

    /** 批次状态，会被 NCR/CAPA 流程推进。 */
    private String status;

    private Long createdBy;

    private String remark;

    /** 数据库创建时间，由 MySQL 默认值维护。 */
    private LocalDateTime createdTime;

    /** 数据库更新时间，由 MySQL ON UPDATE 维护。 */
    private LocalDateTime updatedTime;
}
