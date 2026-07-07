package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ReviewCreateDTO;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.entity.ReviewRecord;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.DetectionResultMapper;
import com.example.visualqms.mapper.ReviewRecordMapper;
import com.example.visualqms.service.ReviewRecordService;
import com.example.visualqms.vo.ReviewRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewRecordServiceImpl
        extends ServiceImpl<ReviewRecordMapper, ReviewRecord>
        implements ReviewRecordService {

    private static final String DETECTION_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final Set<String> ALLOWED_REVIEW_RESULT = Set.of(
            "CONFIRMED_DEFECT",
            "FALSE_POSITIVE",
            "NEED_RECHECK"
    );

    private final DetectionResultMapper detectionResultMapper;

    public ReviewRecordServiceImpl(DetectionResultMapper detectionResultMapper) {
        this.detectionResultMapper = detectionResultMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewRecordVO createReview(ReviewCreateDTO dto) {
        validateReviewResult(dto.getReviewResult());
        validateReviewerExists(dto.getReviewerId());

        DetectionResult detectionResult = getExistingDetectionResult(dto.getDetectionResultId());
        if (!DETECTION_STATUS_PENDING_REVIEW.equals(detectionResult.getStatus())) {
            throw new BizException(400, "only PENDING_REVIEW detection result can be reviewed");
        }
        validateDetectionResultNotReviewed(dto.getDetectionResultId());

        LocalDateTime now = LocalDateTime.now();
        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setDetectionResultId(detectionResult.getId());
        reviewRecord.setTaskId(detectionResult.getTaskId());
        reviewRecord.setImageId(detectionResult.getImageId());
        reviewRecord.setReviewerId(dto.getReviewerId());
        reviewRecord.setReviewResult(dto.getReviewResult());
        reviewRecord.setReviewComment(dto.getReviewComment());
        reviewRecord.setReviewedTime(now);
        save(reviewRecord);

        DetectionResult updateEntity = new DetectionResult();
        updateEntity.setId(detectionResult.getId());
        updateEntity.setStatus(dto.getReviewResult());
        detectionResultMapper.updateById(updateEntity);

        return toVO(getById(reviewRecord.getId()));
    }

    @Override
    public PageResult<ReviewRecordVO> pageReviews(
            Long taskId,
            Long imageId,
            Long reviewerId,
            String reviewResult,
            Long pageNo,
            Long pageSize) {
        if (StringUtils.hasText(reviewResult)) {
            validateReviewResult(reviewResult);
        }

        LambdaQueryWrapper<ReviewRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(taskId != null, ReviewRecord::getTaskId, taskId)
                .eq(imageId != null, ReviewRecord::getImageId, imageId)
                .eq(reviewerId != null, ReviewRecord::getReviewerId, reviewerId)
                .eq(StringUtils.hasText(reviewResult), ReviewRecord::getReviewResult, reviewResult)
                .orderByDesc(ReviewRecord::getReviewedTime)
                .orderByDesc(ReviewRecord::getId);

        Page<ReviewRecord> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<ReviewRecordVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public ReviewRecordVO getReviewDetail(Long id) {
        ReviewRecord reviewRecord = getById(id);
        if (reviewRecord == null) {
            throw new BizException(404, "review record not found");
        }
        return toVO(reviewRecord);
    }

    @Override
    public ReviewRecordVO getReviewByDetectionResult(Long detectionResultId) {
        getExistingDetectionResult(detectionResultId);

        LambdaQueryWrapper<ReviewRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReviewRecord::getDetectionResultId, detectionResultId);
        ReviewRecord reviewRecord = getOne(queryWrapper);
        return reviewRecord == null ? null : toVO(reviewRecord);
    }

    private DetectionResult getExistingDetectionResult(Long detectionResultId) {
        DetectionResult detectionResult = detectionResultMapper.selectById(detectionResultId);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    private void validateReviewerExists(Long reviewerId) {
        Long count = baseMapper.countSysUserById(reviewerId);
        if (count == null || count == 0) {
            throw new BizException(404, "reviewer not found");
        }
    }

    private void validateDetectionResultNotReviewed(Long detectionResultId) {
        LambdaQueryWrapper<ReviewRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReviewRecord::getDetectionResultId, detectionResultId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "detection result already reviewed");
        }
    }

    private void validateReviewResult(String reviewResult) {
        if (!ALLOWED_REVIEW_RESULT.contains(reviewResult)) {
            throw new BizException(400, "invalid review result");
        }
    }

    private ReviewRecordVO toVO(ReviewRecord reviewRecord) {
        ReviewRecordVO vo = new ReviewRecordVO();
        BeanUtils.copyProperties(reviewRecord, vo);
        return vo;
    }
}
