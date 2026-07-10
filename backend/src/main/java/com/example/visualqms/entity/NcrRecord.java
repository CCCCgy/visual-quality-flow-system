package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 ncr_record 不合格记录表，承接已确认缺陷并进入质量问题处理。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * NcrRecordServiceImpl 创建、查询和更新；CapaRecordServiceImpl 创建/关闭 CAPA 时同步更新 NCR 状态。
 *
 * 下游依赖：
 * NcrRecordMapper 访问 ncr_record。
 *
 * 表关系：
 * reviewId 唯一指向 review_record.id；detectionResultId/taskId/batchId 保留完整追溯链。
 *
 * 状态字段：
 * status 可为 OPEN/CAPA_CREATED/CLOSED/CANCELLED。
 */
@Data
@TableName("ncr_record")
public class NcrRecord {

    /** ncr_record 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private String ncrNo;

    private Long batchId;

    private Long taskId;

    private Long detectionResultId;

    private Long reviewId;

    private String severity;

    /** NCR 状态，创建后 OPEN，创建 CAPA 后 CAPA_CREATED，闭环后 CLOSED。 */
    private String status;

    private String description;

    private Long createdBy;

    /** NCR 首次关闭时写入。 */
    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
