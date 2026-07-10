package com.example.visualqms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 映射 inspection_image 检测图片表，记录一次任务下被检测的图片资源。
 *
 * 所属层级：
 * Entity。
 *
 * 上游调用：
 * DetectionImportServiceImpl 根据 YOLO JSON 的 sourceName 创建或复用图片记录；
 * DetectionResultServiceImpl 查询视觉详情时读取图片名称和 URI。
 *
 * 下游依赖：
 * InspectionImageMapper 访问 inspection_image。
 *
 * 表关系：
 * taskId 指向 inspection_task.id；batchId 指向 production_batch.id；id 被 detection_result.image_id 和 review_record.image_id 引用。
 */
@Data
@TableName("inspection_image")
public class InspectionImage {

    /** inspection_image 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long batchId;

    private String imageName;

    /** 前端或后续资源服务可使用的图片相对路径/资源 key。 */
    private String imageUri;

    private Integer width;

    private Integer height;

    /** 图片状态；导入检测结果后由 PENDING 变为 DETECTED。 */
    private String status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
