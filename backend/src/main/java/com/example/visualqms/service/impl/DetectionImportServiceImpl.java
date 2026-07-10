package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.visualqms.dto.DetectionImportDTO;
import com.example.visualqms.dto.YoloBoxDTO;
import com.example.visualqms.dto.YoloDetectionJsonDTO;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.entity.InspectionImage;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.DetectionResultMapper;
import com.example.visualqms.mapper.InspectionImageMapper;
import com.example.visualqms.mapper.InspectionTaskMapper;
import com.example.visualqms.service.DetectionImportService;
import com.example.visualqms.vo.DetectionImportResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件职责：
 * 将前端或接口调用方提交的 YOLO JSON 转换为检测图片和检测结果记录。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * DetectionController#importYoloJson。
 *
 * 下游依赖：
 * InspectionTaskMapper、InspectionImageMapper、DetectionResultMapper，最终访问
 * inspection_task、inspection_image、detection_result。
 *
 * 主要业务链路：
 * 接口调用方 -> POST /api/detections/import-json -> DetectionController
 * -> DetectionImportServiceImpl -> inspection_task / inspection_image / detection_result。
 *
 * 注意事项：
 * 该类负责一次导入中的多表一致性，因此核心方法使用事务；任一检测框落库失败时，图片状态和任务状态也应整体回滚。
 */
@Service
public class DetectionImportServiceImpl implements DetectionImportService {

    private static final String TASK_STATUS_WAIT_REVIEW = "WAIT_REVIEW";
    private static final String TASK_STATUS_CLOSED = "CLOSED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String IMAGE_STATUS_DETECTED = "DETECTED";
    private static final String DETECTION_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String IMAGE_URI_PREFIX = "sample-data/images/";

    private final InspectionTaskMapper inspectionTaskMapper;
    private final InspectionImageMapper inspectionImageMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final ObjectMapper objectMapper;

    public DetectionImportServiceImpl(
            InspectionTaskMapper inspectionTaskMapper,
            InspectionImageMapper inspectionImageMapper,
            DetectionResultMapper detectionResultMapper,
            ObjectMapper objectMapper) {
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.inspectionImageMapper = inspectionImageMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 导入 YOLO JSON。
     *
     * 前置条件：
     * taskId 必须存在；CLOSED/CANCELLED 任务不能导入；boxes 必须非空且每个 bbox 包含 4 个坐标。
     *
     * 查询数据：
     * 读取 inspection_task；按 taskId + sourceName 查询 inspection_image，存在则复用，不存在则创建。
     *
     * 写入数据：
     * 每个 box 转成一条 detection_result；inspection_image.status 更新为 DETECTED；
     * inspection_task.status 更新为 WAIT_REVIEW，同时写入 imported_time。
     *
     * 状态变化：
     * detection_result 默认为 PENDING_REVIEW，因为模型输出还没有人工确认；
     * inspection_image 变为 DETECTED，表示该图片已有模型检测结果；
     * inspection_task 变为 WAIT_REVIEW，提示前端进入人工复核环节。
     *
     * 事务说明：
     * 三张表必须一致；若中途异常，已插入的 detection_result 和状态更新会一起回滚。
     *
     * @param dto taskId 与 YOLO JSON 内容
     * @return 导入统计结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DetectionImportResultVO importYoloJson(DetectionImportDTO dto) {
        InspectionTask task = getImportableTask(dto.getTaskId());
        YoloDetectionJsonDTO yoloJson = dto.getYoloJson();
        validateBoxes(yoloJson.getBoxes());

        InspectionImage image = getOrCreateImage(task, yoloJson.getSourceName());

        for (YoloBoxDTO box : yoloJson.getBoxes()) {
            detectionResultMapper.insert(toDetectionResult(task.getId(), image.getId(), box));
        }

        updateImageDetected(image.getId());
        updateTaskWaitReview(task.getId());

        DetectionImportResultVO result = new DetectionImportResultVO();
        result.setTaskId(task.getId());
        result.setImageId(image.getId());
        result.setImageCount(1);
        result.setDetectionCount(yoloJson.getBoxes().size());
        return result;
    }

    /**
     * 获取可导入的检测任务，并阻止终态任务继续追加检测结果。
     */
    private InspectionTask getImportableTask(Long taskId) {
        InspectionTask task = inspectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(404, "inspection task not found");
        }
        if (TASK_STATUS_CLOSED.equals(task.getStatus()) || TASK_STATUS_CANCELLED.equals(task.getStatus())) {
            throw new BizException("CLOSED or CANCELLED inspection task cannot import detections");
        }
        return task;
    }

    /**
     * 根据 sourceName 找到或创建检测图片。
     *
     * 业务意义：
     * sourceName 对应 YOLO JSON 中的图片文件名，并落入 inspection_image.image_name；
     * image_uri 使用统一前缀，供视觉详情接口返回给前端。
     */
    private InspectionImage getOrCreateImage(InspectionTask task, String sourceName) {
        LambdaQueryWrapper<InspectionImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InspectionImage::getTaskId, task.getId())
                .eq(InspectionImage::getImageName, sourceName);
        InspectionImage existingImage = inspectionImageMapper.selectOne(queryWrapper);
        if (existingImage != null) {
            return existingImage;
        }

        InspectionImage image = new InspectionImage();
        image.setTaskId(task.getId());
        image.setBatchId(task.getBatchId());
        image.setImageName(sourceName);
        image.setImageUri(IMAGE_URI_PREFIX + sourceName);
        image.setStatus(IMAGE_STATUS_DETECTED);
        inspectionImageMapper.insert(image);
        return image;
    }

    /**
     * 将单个 YOLO box 转为 detection_result。
     *
     * 字段映射：
     * classId/className/confidence 直接保存模型输出；
     * bbox_xyxy 的 4 个数依次保存为 bboxX1/bboxY1/bboxX2/bboxY2；
     * rawPayload 保存原始片段，便于后续排查模型输出和数据库字段的对应关系。
     */
    private DetectionResult toDetectionResult(Long taskId, Long imageId, YoloBoxDTO box) {
        List<BigDecimal> bbox = box.getBboxXyxy();

        DetectionResult result = new DetectionResult();
        result.setTaskId(taskId);
        result.setImageId(imageId);
        result.setClassId(box.getClassId());
        result.setClassName(box.getClassName());
        result.setConfidence(box.getConfidence());
        result.setBboxX1(bbox.get(0));
        result.setBboxY1(bbox.get(1));
        result.setBboxX2(bbox.get(2));
        result.setBboxY2(bbox.get(3));
        result.setStatus(DETECTION_STATUS_PENDING_REVIEW);
        result.setRawPayload(toRawPayload(box));
        return result;
    }

    /**
     * 标记图片已完成模型检测。
     */
    private void updateImageDetected(Long imageId) {
        InspectionImage updateEntity = new InspectionImage();
        updateEntity.setId(imageId);
        updateEntity.setStatus(IMAGE_STATUS_DETECTED);
        inspectionImageMapper.updateById(updateEntity);
    }

    /**
     * 标记任务等待人工复核，并记录导入时间。
     */
    private void updateTaskWaitReview(Long taskId) {
        InspectionTask updateEntity = new InspectionTask();
        updateEntity.setId(taskId);
        updateEntity.setStatus(TASK_STATUS_WAIT_REVIEW);
        updateEntity.setImportedTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(updateEntity);
    }

    /**
     * 校验 YOLO boxes 的最小结构，避免缺少坐标时写出无效 bbox。
     */
    private void validateBoxes(List<YoloBoxDTO> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            throw new BizException(400, "boxes cannot be empty");
        }
        for (YoloBoxDTO box : boxes) {
            if (box == null) {
                throw new BizException(400, "box cannot be null");
            }
            if (box.getBboxXyxy().size() != 4) {
                throw new BizException(400, "bboxXyxy must contain 4 values");
            }
        }
    }

    /**
     * 将单个检测框序列化为 raw_payload，保存模型原始字段以便追溯。
     */
    private String toRawPayload(YoloBoxDTO box) {
        try {
            Map<String, Object> rawPayload = new LinkedHashMap<>();
            rawPayload.put("class_id", box.getClassId());
            rawPayload.put("class_name", box.getClassName());
            rawPayload.put("confidence", box.getConfidence());
            rawPayload.put("bbox_xyxy", box.getBboxXyxy());
            return objectMapper.writeValueAsString(rawPayload);
        } catch (JsonProcessingException exception) {
            throw new BizException("failed to serialize detection box");
        }
    }
}
