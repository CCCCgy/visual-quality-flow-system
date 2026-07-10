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

/**
 * 文件职责：
 * 实现检测结果查询和视觉详情查询。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * DetectionController。
 *
 * 下游依赖：
 * DetectionResultMapper 访问 detection_result；InspectionImageMapper 访问 inspection_image。
 *
 * 主要业务链路：
 * DetectionResultDetailView.vue -> detectionApi.js -> DetectionController
 * -> DetectionResultServiceImpl -> detection_result / inspection_image -> DetectionVisualDetailVO。
 *
 * 注意事项：
 * 本类只读检测结果；检测结果状态写入由 ReviewRecordServiceImpl 在人工复核事务中完成。
 */
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

    /**
     * 分页查询检测结果。
     *
     * 查询数据：
     * detection_result，可按 taskId、imageId、className 和 status 过滤。
     */
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

    /**
     * 查询检测结果基础详情。
     */
    @Override
    public DetectionResultVO getDetectionResultDetail(Long id) {
        return toVO(getExistingDetectionResult(id));
    }

    /**
     * 查询视觉详情。
     *
     * 查询数据：
     * 先查 detection_result，再通过 imageId 查 inspection_image。
     *
     * 输出数据：
     * DetectionVisualDetailVO 合并检测类别、置信度、bbox 坐标、状态与图片名称/URI。
     * 前端根据 bboxX1/bboxY1/bboxX2/bboxY2 在图片上绘制矩形框。
     */
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

    /**
     * 校验检测结果状态枚举。
     */
    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid detection result status");
        }
    }

    /**
     * 获取已存在检测结果。
     */
    private DetectionResult getExistingDetectionResult(Long id) {
        DetectionResult detectionResult = getById(id);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    /**
     * Entity 转 VO，避免暴露持久化对象。
     */
    private DetectionResultVO toVO(DetectionResult detectionResult) {
        DetectionResultVO vo = new DetectionResultVO();
        BeanUtils.copyProperties(detectionResult, vo);
        return vo;
    }
}
