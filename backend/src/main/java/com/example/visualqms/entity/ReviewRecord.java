package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 review_record 人工复核记录表，保存人工对模型检测结果的结论。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * ReviewRecordServiceImpl 创建和查询；NcrRecordServiceImpl 创建 NCR 时追溯复核结论。
 *
 * 下游依赖：
 * ReviewRecordMapper 访问 review_record。
 *
 * 表关系：
 * detectionResultId 唯一指向 detection_result.id；taskId/imageId 冗余保存追溯字段；reviewerId 逻辑指向 sys_user.id。
 *
 * 状态字段：
 * reviewResult 是人工结论，只有 CONFIRMED_DEFECT 可继续创建 NCR。
 */
@Data
@TableName("review_record")
public class ReviewRecord {

    /** review_record 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long detectionResultId;

    private Long taskId;

    private Long imageId;

    private Long reviewerId;

    /** 人工复核结论：确认缺陷、误报或需要复检。 */
    private String reviewResult;

    private String reviewComment;

    /** 人工提交复核的时间，由 Service 写入。 */
    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
