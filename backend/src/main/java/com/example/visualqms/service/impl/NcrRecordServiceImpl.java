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

/**
 * 文件职责：
 * 实现 NCR 不合格记录业务，将确认缺陷的复核结果升级为质量问题单。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * NcrRecordController。
 *
 * 下游依赖：
 * ReviewRecordMapper、DetectionResultMapper、InspectionTaskMapper、ProductionBatchMapper 与 NcrRecordMapper，
 * 最终访问 review_record、detection_result、inspection_task、production_batch、ncr_record。
 *
 * 主要业务链路：
 * ReviewListView.vue -> ncrApi.js -> NcrRecordController -> NcrRecordServiceImpl
 * -> review_record / detection_result / inspection_task / ncr_record / production_batch。
 *
 * 注意事项：
 * NCR 是从人工确认缺陷产生的质量问题单；只有 CONFIRMED_DEFECT 能进入本流程。
 */
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

    /**
     * 创建 NCR。
     *
     * 前置条件：
     * ncrNo 唯一；createdBy 必须存在于 sys_user；
     * review_record 必须存在且 reviewResult=CONFIRMED_DEFECT；
     * 同一 review_record 只能关联一条 NCR。
     *
     * 查询数据：
     * review_record 追溯 detection_result，再追溯 inspection_task 和 production_batch，
     * 用于确认批次 ID 来自任务而不是前端自行传入。
     *
     * 写入数据：
     * 新增 ncr_record，默认 status=OPEN；
     * 更新 production_batch.status=NCR_OPEN。
     *
     * 事务说明：
     * NCR 创建和批次状态推进必须一致；若批次更新失败，NCR 插入也应回滚。
     */
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

    /**
     * 分页查询 NCR，供 NcrListView 展示和筛选。
     */
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

    /**
     * 查询 NCR 详情。
     */
    @Override
    public NcrRecordVO getNcrDetail(Long id) {
        return toVO(getExistingNcrRecord(id));
    }

    /**
     * 根据复核记录查询 NCR，用于判断 review_record 是否已进入不合格流程。
     */
    @Override
    public NcrRecordVO getNcrByReview(Long reviewId) {
        getExistingReviewRecord(reviewId);

        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getReviewId, reviewId);
        NcrRecord ncrRecord = getOne(queryWrapper);
        return ncrRecord == null ? null : toVO(ncrRecord);
    }

    /**
     * 更新 NCR 状态。
     *
     * 状态限制：
     * 只允许 OPEN、CLOSED、CANCELLED 作为人工更新目标；
     * CAPA_CREATED 由创建 CAPA 时自动写入，不通过此接口直接设置。
     *
     * 写入数据：
     * 更新 ncr_record.status；首次关闭时写入 closed_time。
     */
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

    /**
     * 校验 NCR 编号唯一性，对应 ncr_record.uk_ncr_record_ncr_no。
     */
    private void validateNcrNoUnique(String ncrNo) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getNcrNo, ncrNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "ncrNo already exists");
        }
    }

    /**
     * 校验 NCR 创建人存在性；当前通过自定义 SQL 查询 sys_user。
     */
    private void validateCreatedByExists(Long createdBy) {
        Long count = baseMapper.countSysUserById(createdBy);
        if (count == null || count == 0) {
            throw new BizException(404, "createdBy user not found");
        }
    }

    /**
     * 防止同一复核记录重复创建 NCR，对应 ncr_record.uk_ncr_review_id。
     */
    private void validateReviewNotLinkedToNcr(Long reviewId) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getReviewId, reviewId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "review already has NCR");
        }
    }

    /**
     * 获取已存在 NCR。
     */
    private NcrRecord getExistingNcrRecord(Long id) {
        NcrRecord ncrRecord = getById(id);
        if (ncrRecord == null) {
            throw new BizException(404, "NCR record not found");
        }
        return ncrRecord;
    }

    /**
     * 获取已存在复核记录，作为 NCR 的直接来源。
     */
    private ReviewRecord getExistingReviewRecord(Long reviewId) {
        ReviewRecord reviewRecord = reviewRecordMapper.selectById(reviewId);
        if (reviewRecord == null) {
            throw new BizException(404, "review record not found");
        }
        return reviewRecord;
    }

    /**
     * 获取已存在检测结果，用于校验复核记录和任务之间的一致性。
     */
    private DetectionResult getExistingDetectionResult(Long detectionResultId) {
        DetectionResult detectionResult = detectionResultMapper.selectById(detectionResultId);
        if (detectionResult == null) {
            throw new BizException(404, "detection result not found");
        }
        return detectionResult;
    }

    /**
     * 获取已存在检测任务，NCR 的 batchId 从该任务追溯得到。
     */
    private InspectionTask getExistingInspectionTask(Long taskId) {
        InspectionTask inspectionTask = inspectionTaskMapper.selectById(taskId);
        if (inspectionTask == null) {
            throw new BizException(404, "inspection task not found");
        }
        return inspectionTask;
    }

    /**
     * 获取已存在批次，确保即将推进到 NCR_OPEN 的生产批次真实存在。
     */
    private ProductionBatch getExistingProductionBatch(Long batchId) {
        ProductionBatch productionBatch = productionBatchMapper.selectById(batchId);
        if (productionBatch == null) {
            throw new BizException(404, "production batch not found");
        }
        return productionBatch;
    }

    /**
     * 校验查询条件中的 NCR 状态。
     */
    private void validateQueryStatus(String status) {
        if (!ALLOWED_QUERY_STATUS.contains(status)) {
            throw new BizException(400, "invalid NCR status");
        }
    }

    /**
     * 校验人工状态更新允许的目标。
     */
    private void validateUpdateStatus(String status) {
        if (!ALLOWED_UPDATE_STATUS.contains(status)) {
            throw new BizException(400, "invalid NCR status");
        }
    }

    /**
     * 判断 NCR 是否处于不可逆终态。
     */
    private boolean isTerminalStatus(String status) {
        return NCR_STATUS_CLOSED.equals(status) || NCR_STATUS_CANCELLED.equals(status);
    }

    /**
     * Entity 转 VO，避免 Controller 暴露持久化对象。
     */
    private NcrRecordVO toVO(NcrRecord ncrRecord) {
        NcrRecordVO vo = new NcrRecordVO();
        BeanUtils.copyProperties(ncrRecord, vo);
        return vo;
    }
}
