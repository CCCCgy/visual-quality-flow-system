package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.NcrCreateDTO;
import com.example.visualqms.dto.NcrStatusUpdateDTO;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.entity.NcrRecord;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.entity.ReviewRecord;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.DetectionResultMapper;
import com.example.visualqms.mapper.InspectionTaskMapper;
import com.example.visualqms.mapper.NcrRecordMapper;
import com.example.visualqms.mapper.ProductionBatchMapper;
import com.example.visualqms.mapper.ReviewRecordMapper;
import com.example.visualqms.service.NcrRecordService;
import com.example.visualqms.vo.NcrRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NcrRecordServiceImpl
        extends ServiceImpl<NcrRecordMapper, NcrRecord>
        implements NcrRecordService {

    private static final String REVIEW_RESULT_CONFIRMED_DEFECT = "CONFIRMED_DEFECT";
    private static final String NCR_STATUS_OPEN = "OPEN";
    private static final String NCR_STATUS_CAPA_CREATED = "CAPA_CREATED";
    private static final String NCR_STATUS_CLOSED = "CLOSED";
    private static final String NCR_STATUS_CANCELLED = "CANCELLED";
    private static final String BATCH_STATUS_NCR_OPEN = "NCR_OPEN";
    private static final Set<String> ALLOWED_QUERY_STATUS = Set.of(
            NCR_STATUS_OPEN,
            NCR_STATUS_CAPA_CREATED,
            NCR_STATUS_CLOSED,
            NCR_STATUS_CANCELLED
    );
    private static final Set<String> ALLOWED_UPDATE_STATUS = Set.of(
            NCR_STATUS_OPEN,
            NCR_STATUS_CLOSED,
            NCR_STATUS_CANCELLED
    );

    private final ReviewRecordMapper reviewRecordMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final InspectionTaskMapper inspectionTaskMapper;
    private final ProductionBatchMapper productionBatchMapper;

    public NcrRecordServiceImpl(
            ReviewRecordMapper reviewRecordMapper,
            DetectionResultMapper detectionResultMapper,
            InspectionTaskMapper inspectionTaskMapper,
            ProductionBatchMapper productionBatchMapper) {
        this.reviewRecordMapper = reviewRecordMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.productionBatchMapper = productionBatchMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NcrRecordVO createNcr(NcrCreateDTO dto) {
        validateNcrNoUnique(dto.getNcrNo());
        validateCreatedByExists(dto.getCreatedBy());

        ReviewRecord reviewRecord = getExistingReviewRecord(dto.getReviewId());
        if (!REVIEW_RESULT_CONFIRMED_DEFECT.equals(reviewRecord.getReviewResult())) {
            throw new BizException(400, "only CONFIRMED_DEFECT review can create NCR");
        }
        validateReviewNotLinkedToNcr(dto.getReviewId());

        DetectionResult detectionResult = getExistingDetectionResult(reviewRecord.getDetectionResultId());
        InspectionTask inspectionTask = getExistingInspectionTask(reviewRecord.getTaskId());
        ProductionBatch productionBatch = getExistingProductionBatch(inspectionTask.getBatchId());

        if (!inspectionTask.getId().equals(detectionResult.getTaskId())) {
            throw new BizException(400, "review, detection result, and inspection task are inconsistent");
        }
        if (!productionBatch.getId().equals(inspectionTask.getBatchId())) {
            throw new BizException(400, "inspection task and production batch are inconsistent");
        }

        NcrRecord ncrRecord = new NcrRecord();
        ncrRecord.setNcrNo(dto.getNcrNo());
        ncrRecord.setBatchId(inspectionTask.getBatchId());
        ncrRecord.setTaskId(reviewRecord.getTaskId());
        ncrRecord.setDetectionResultId(reviewRecord.getDetectionResultId());
        ncrRecord.setReviewId(dto.getReviewId());
        ncrRecord.setSeverity(dto.getSeverity());
        ncrRecord.setStatus(NCR_STATUS_OPEN);
        ncrRecord.setDescription(dto.getDescription());
        ncrRecord.setCreatedBy(dto.getCreatedBy());
        save(ncrRecord);

        ProductionBatch updateBatch = new ProductionBatch();
        updateBatch.setId(inspectionTask.getBatchId());
        updateBatch.setStatus(BATCH_STATUS_NCR_OPEN);
        productionBatchMapper.updateById(updateBatch);

        return toVO(getById(ncrRecord.getId()));
    }

    @Override
    public PageResult<NcrRecordVO> pageNcrs(
            String ncrNo,
            Long batchId,
            Long taskId,
            String severity,
            String status,
            Long pageNo,
            Long pageSize) {
        if (StringUtils.hasText(status)) {
            validateQueryStatus(status);
        }

        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(ncrNo), NcrRecord::getNcrNo, ncrNo)
                .eq(batchId != null, NcrRecord::getBatchId, batchId)
                .eq(taskId != null, NcrRecord::getTaskId, taskId)
                .eq(StringUtils.hasText(severity), NcrRecord::getSeverity, severity)
                .eq(StringUtils.hasText(status), NcrRecord::getStatus, status)
                .orderByDesc(NcrRecord::getCreatedTime)
                .orderByDesc(NcrRecord::getId);

        Page<NcrRecord> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<NcrRecordVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public NcrRecordVO getNcrDetail(Long id) {
        return toVO(getExistingNcrRecord(id));
    }

    @Override
    public NcrRecordVO getNcrByReview(Long reviewId) {
        getExistingReviewRecord(reviewId);

        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getReviewId, reviewId);
        NcrRecord ncrRecord = getOne(queryWrapper);
        return ncrRecord == null ? null : toVO(ncrRecord);
    }

    @Override
    public NcrRecordVO updateNcrStatus(Long id, NcrStatusUpdateDTO dto) {
        NcrRecord ncrRecord = getExistingNcrRecord(id);
        String targetStatus = dto.getStatus();
        validateUpdateStatus(targetStatus);

        if (isTerminalStatus(ncrRecord.getStatus()) && !ncrRecord.getStatus().equals(targetStatus)) {
            throw new BizException(400, "terminal status NCR cannot switch to another status");
        }

        NcrRecord updateEntity = new NcrRecord();
        updateEntity.setId(id);
        updateEntity.setStatus(targetStatus);
        if (!NCR_STATUS_CLOSED.equals(ncrRecord.getStatus()) && NCR_STATUS_CLOSED.equals(targetStatus)) {
            updateEntity.setClosedTime(LocalDateTime.now());
        }
        updateById(updateEntity);
        return toVO(getById(id));
    }

    private void validateNcrNoUnique(String ncrNo) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getNcrNo, ncrNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "ncrNo already exists");
        }
    }

    private void validateCreatedByExists(Long createdBy) {
        Long count = baseMapper.countSysUserById(createdBy);
        if (count == null || count == 0) {
            throw new BizException(404, "createdBy user not found");
        }
    }

    private void validateReviewNotLinkedToNcr(Long reviewId) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getReviewId, reviewId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "review already has NCR");
        }
    }

    private NcrRecord getExistingNcrRecord(Long id) {
        NcrRecord ncrRecord = getById(id);
        if (ncrRecord == null) {
            throw new BizException(404, "NCR record not found");
        }
        return ncrRecord;
    }

    private ReviewRecord getExistingReviewRecord(Long reviewId) {
        ReviewRecord reviewRecord = reviewRecordMapper.selectById(reviewId);
        if (reviewRecord == null) {
            throw new BizException(404, "review record not found");
        }
        return reviewRecord;
    }

    private DetectionResult getExistingDetectionResult(Long detectionResultId) {
        DetectionResult detectionResult = detectionResultMapper.selectById(detectionResultId);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    private InspectionTask getExistingInspectionTask(Long taskId) {
        InspectionTask inspectionTask = inspectionTaskMapper.selectById(taskId);
        if (inspectionTask == null) {
            throw new BizException(404, "inspection task not found");
        }
        return inspectionTask;
    }

    private ProductionBatch getExistingProductionBatch(Long batchId) {
        ProductionBatch productionBatch = productionBatchMapper.selectById(batchId);
        if (productionBatch == null) {
            throw new BizException(404, "production batch not found");
        }
        return productionBatch;
    }

    private void validateQueryStatus(String status) {
        if (!ALLOWED_QUERY_STATUS.contains(status)) {
            throw new BizException(400, "invalid NCR status");
        }
    }

    private void validateUpdateStatus(String status) {
        if (!ALLOWED_UPDATE_STATUS.contains(status)) {
            throw new BizException(400, "invalid NCR status");
        }
    }

    private boolean isTerminalStatus(String status) {
        return NCR_STATUS_CLOSED.equals(status) || NCR_STATUS_CANCELLED.equals(status);
    }

    private NcrRecordVO toVO(NcrRecord ncrRecord) {
        NcrRecordVO vo = new NcrRecordVO();
        BeanUtils.copyProperties(ncrRecord, vo);
        return vo;
    }
}
