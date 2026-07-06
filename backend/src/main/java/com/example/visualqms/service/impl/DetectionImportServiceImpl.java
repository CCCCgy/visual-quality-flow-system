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

    private void updateImageDetected(Long imageId) {
        InspectionImage updateEntity = new InspectionImage();
        updateEntity.setId(imageId);
        updateEntity.setStatus(IMAGE_STATUS_DETECTED);
        inspectionImageMapper.updateById(updateEntity);
    }

    private void updateTaskWaitReview(Long taskId) {
        InspectionTask updateEntity = new InspectionTask();
        updateEntity.setId(taskId);
        updateEntity.setStatus(TASK_STATUS_WAIT_REVIEW);
        updateEntity.setImportedTime(LocalDateTime.now());
        inspectionTaskMapper.updateById(updateEntity);
    }

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
