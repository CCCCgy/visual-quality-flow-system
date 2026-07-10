package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 capa_record CAPA 整改记录表，保存纠正预防措施和关闭信息。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * CapaRecordServiceImpl 创建、编辑、查询和关闭 CAPA。
 *
 * 下游依赖：
 * CapaRecordMapper 访问 capa_record。
 *
 * 表关系：
 * ncrId 唯一指向 ncr_record.id；batchId 指向 production_batch.id；ownerId 逻辑指向 sys_user.id。
 *
 * 状态字段：
 * status 可为 PENDING_ANALYSIS/IN_PROGRESS/PENDING_VERIFY/CLOSED/CANCELLED；
 * 关闭 CAPA 会联动关闭 NCR 和批次。
 */
@Data
@TableName("capa_record")
public class CapaRecord {

    /** capa_record 主键，自增。 */
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

    /** CAPA 状态，决定是否还能编辑和是否触发闭环关闭。 */
    private String status;

    private LocalDate dueDate;

    /** CAPA 首次关闭时写入。 */
    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
