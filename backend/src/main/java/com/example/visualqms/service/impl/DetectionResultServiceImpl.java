package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.entity.InspectionImage;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.DetectionResultMapper;
import com.example.visualqms.mapper.InspectionImageMapper;
import com.example.visualqms.service.DetectionResultService;
import com.example.visualqms.vo.DetectionResultVO;
import com.example.visualqms.vo.DetectionVisualDetailVO;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DetectionResultServiceImpl
        extends ServiceImpl<DetectionResultMapper, DetectionResult>
        implements DetectionResultService {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "PENDING_REVIEW",
            "CONFIRMED_DEFECT",
            "FALSE_POSITIVE",
            "NEED_RECHECK"
    );

    private final InspectionImageMapper inspectionImageMapper;

    public DetectionResultServiceImpl(InspectionImageMapper inspectionImageMapper) {
        this.inspectionImageMapper = inspectionImageMapper;
    }

    @Override
    public PageResult<DetectionResultVO> pageDetectionResults(
            Long taskId,
            Long imageId,
            String className,
            String status,
            Long pageNo,
            Long pageSize) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }

        LambdaQueryWrapper<DetectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(taskId != null, DetectionResult::getTaskId, taskId)
                .eq(imageId != null, DetectionResult::getImageId, imageId)
                .eq(StringUtils.hasText(className), DetectionResult::getClassName, className)
                .eq(StringUtils.hasText(status), DetectionResult::getStatus, status)
                .orderByDesc(DetectionResult::getCreatedTime)
                .orderByDesc(DetectionResult::getId);

        Page<DetectionResult> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<DetectionResultVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public DetectionResultVO getDetectionResultDetail(Long id) {
        return toVO(getExistingDetectionResult(id));
    }

    @Override
    public DetectionVisualDetailVO getDetectionVisualDetail(Long id) {
        DetectionResult detectionResult = getExistingDetectionResult(id);
        InspectionImage inspectionImage = inspectionImageMapper.selectById(detectionResult.getImageId());
        if (inspectionImage == null) {
            throw new BizException(404, "inspection image not found");
        }

        DetectionVisualDetailVO vo = new DetectionVisualDetailVO();
        BeanUtils.copyProperties(detectionResult, vo);
        vo.setImageName(inspectionImage.getImageName());
        vo.setImageUri(inspectionImage.getImageUri());
        return vo;
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid detection result status");
        }
    }

    private DetectionResult getExistingDetectionResult(Long id) {
        DetectionResult detectionResult = getById(id);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    private DetectionResultVO toVO(DetectionResult detectionResult) {
        DetectionResultVO vo = new DetectionResultVO();
        BeanUtils.copyProperties(detectionResult, vo);
        return vo;
    }
}
