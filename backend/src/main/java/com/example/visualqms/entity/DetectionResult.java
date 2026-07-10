package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 detection_result 检测结果表，保存模型输出的类别、置信度和 bbox 坐标。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * DetectionImportServiceImpl 写入模型检测结果；DetectionResultServiceImpl 查询；
 * ReviewRecordServiceImpl 在人工复核后同步更新 status。
 *
 * 下游依赖：
 * DetectionResultMapper 访问 detection_result。
 *
 * 表关系：
 * taskId 指向 inspection_task.id；imageId 指向 inspection_image.id；id 被 review_record.detection_result_id 和 ncr_record.detection_result_id 引用。
 *
 * 状态字段：
 * status 初始为 PENDING_REVIEW，人工复核后同步为 CONFIRMED_DEFECT/FALSE_POSITIVE/NEED_RECHECK。
 */
@Data
@TableName("detection_result")
public class DetectionResult {

    /** detection_result 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long imageId;

    private Integer classId;

    private String className;

    /** 模型置信度，来自 YOLO JSON 的 confidence。 */
    private BigDecimal confidence;

    /** bbox 左上角 x 坐标，来自 bbox_xyxy[0]。 */
    private BigDecimal bboxX1;

    /** bbox 左上角 y 坐标，来自 bbox_xyxy[1]。 */
    private BigDecimal bboxY1;

    /** bbox 右下角 x 坐标，来自 bbox_xyxy[2]。 */
    private BigDecimal bboxX2;

    /** bbox 右下角 y 坐标，来自 bbox_xyxy[3]。 */
    private BigDecimal bboxY2;

    /** 检测结果状态，人工复核后会被同步改写。 */
    private String status;

    /** 原始检测框 JSON 片段，用于追溯模型输出。 */
    private String rawPayload;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
