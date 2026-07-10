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

/**
 * 文件职责：
 * 实现人工复核业务，将模型检测结果转成人工确认结论。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * ReviewRecordController。
 *
 * 下游依赖：
 * ReviewRecordMapper 写入 review_record；DetectionResultMapper 更新 detection_result.status；
 * ReviewRecordMapper 的自定义 SQL 校验 sys_user 中复核人是否存在。
 *
 * 主要业务链路：
 * DetectionResultListView.vue 或 DetectionResultDetailView.vue -> reviewApi.js
 * -> ReviewRecordController -> ReviewRecordServiceImpl -> review_record / detection_result。
 *
 * 注意事项：
 * 模型结果保存在 detection_result，人工结论保存在 review_record；两者分开能保留模型原始输出并记录人工责任人。
 */
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

    /**
     * 创建人工复核记录。
     *
     * 前置条件：
     * reviewResult 必须为 CONFIRMED_DEFECT、FALSE_POSITIVE 或 NEED_RECHECK；
     * reviewerId 必须存在于 sys_user；
     * detection_result 必须存在且状态为 PENDING_REVIEW；
     * 同一 detection_result 只能复核一次。
     *
     * 写入数据：
     * 新增 review_record；同步更新 detection_result.status 为人工复核结论。
     *
     * 状态含义：
     * CONFIRMED_DEFECT 表示确认缺陷，可进入 NCR；
     * FALSE_POSITIVE 表示模型误报；
     * NEED_RECHECK 表示需要重新检查，暂不进入 NCR。
     *
     * 事务说明：
     * review_record 与 detection_result.status 必须一致，任一步失败都整体回滚。
     */
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

    /**
     * 分页查询复核记录，供 ReviewListView 展示和筛选。
     */
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

    /**
     * 查询复核详情。
     */
    @Override
    public ReviewRecordVO getReviewDetail(Long id) {
        ReviewRecord reviewRecord = getById(id);
        if (reviewRecord == null) {
            throw new BizException(404, "review record not found");
        }
        return toVO(reviewRecord);
    }

    /**
     * 根据 detection_result 查询复核记录，便于调用方判断某检测结果是否已复核。
     */
    @Override
    public ReviewRecordVO getReviewByDetectionResult(Long detectionResultId) {
        getExistingDetectionResult(detectionResultId);

        LambdaQueryWrapper<ReviewRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReviewRecord::getDetectionResultId, detectionResultId);
        ReviewRecord reviewRecord = getOne(queryWrapper);
        return reviewRecord == null ? null : toVO(reviewRecord);
    }

    /**
     * 校验检测结果存在性。
     */
    private DetectionResult getExistingDetectionResult(Long detectionResultId) {
        DetectionResult detectionResult = detectionResultMapper.selectById(detectionResultId);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    /**
     * 校验复核人存在性；当前系统没有 User Entity，因此通过 Mapper 自定义 SQL 查询 sys_user。
     */
    private void validateReviewerExists(Long reviewerId) {
        Long count = baseMapper.countSysUserById(reviewerId);
        if (count == null || count == 0) {
            throw new BizException(404, "reviewer not found");
        }
    }

    /**
     * 防止同一模型检测结果被重复复核，和 review_record 的唯一索引保持一致。
     */
    private void validateDetectionResultNotReviewed(Long detectionResultId) {
        LambdaQueryWrapper<ReviewRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReviewRecord::getDetectionResultId, detectionResultId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "detection result already reviewed");
        }
    }

    /**
     * 校验人工复核结论枚举。
     */
    private void validateReviewResult(String reviewResult) {
        if (!ALLOWED_REVIEW_RESULT.contains(reviewResult)) {
            throw new BizException(400, "invalid review result");
        }
    }

    /**
     * Entity 转 VO，控制返回给前端的字段形态。
     */
    private ReviewRecordVO toVO(ReviewRecord reviewRecord) {
        ReviewRecordVO vo = new ReviewRecordVO();
        BeanUtils.copyProperties(reviewRecord, vo);
        return vo;
    }
}
