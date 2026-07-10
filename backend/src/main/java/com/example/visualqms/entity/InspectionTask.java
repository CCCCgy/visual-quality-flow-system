package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 inspection_task 检测任务表，连接生产批次与一次模型检测导入。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * InspectionTaskServiceImpl 创建和查询；DetectionImportServiceImpl 导入 YOLO JSON 时更新任务状态和导入时间。
 *
 * 下游依赖：
 * InspectionTaskMapper 基于该实体访问 inspection_task。
 *
 * 表关系：
 * batchId 逻辑指向 production_batch.id；id 被 inspection_image.task_id、detection_result.task_id、review_record.task_id、ncr_record.task_id 引用。
 *
 * 状态字段：
 * status 表示任务是否已创建、等待复核、已复核、关闭或取消。
 */
@Data
@TableName("inspection_task")
public class InspectionTask {

    /** inspection_task 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务业务编号，数据库唯一。 */
    private String taskNo;

    /** 所属生产批次 ID。 */
    private Long batchId;

    private String modelName;

    private String modelVersion;

    private String sourceType;

    /** 任务状态，导入检测结果后通常从 CREATED 变为 WAIT_REVIEW。 */
    private String status;

    private Long createdBy;

    /** YOLO JSON 成功导入时写入。 */
    private LocalDateTime importedTime;

    /** 预留的复核完成时间字段，当前核心闭环未主动写入。 */
    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
