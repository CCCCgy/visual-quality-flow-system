package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.CapaCreateDTO;
import com.example.visualqms.dto.CapaStatusUpdateDTO;
import com.example.visualqms.dto.CapaUpdateDTO;
import com.example.visualqms.entity.CapaRecord;
import com.example.visualqms.entity.NcrRecord;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.CapaRecordMapper;
import com.example.visualqms.mapper.NcrRecordMapper;
import com.example.visualqms.mapper.ProductionBatchMapper;
import com.example.visualqms.service.CapaRecordService;
import com.example.visualqms.vo.CapaRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CapaRecordServiceImpl
        extends ServiceImpl<CapaRecordMapper, CapaRecord>
        implements CapaRecordService {

    private static final String NCR_STATUS_OPEN = "OPEN";
    private static final String NCR_STATUS_CAPA_CREATED = "CAPA_CREATED";
    private static final String NCR_STATUS_CLOSED = "CLOSED";
    private static final String BATCH_STATUS_CAPA_OPEN = "CAPA_OPEN";
    private static final String BATCH_STATUS_CLOSED = "CLOSED";
    private static final String CAPA_STATUS_PENDING_ANALYSIS = "PENDING_ANALYSIS";
    private static final String CAPA_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String CAPA_STATUS_PENDING_VERIFY = "PENDING_VERIFY";
    private static final String CAPA_STATUS_CLOSED = "CLOSED";
    private static final String CAPA_STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> ALLOWED_STATUS = Set.of(
            CAPA_STATUS_PENDING_ANALYSIS,
            CAPA_STATUS_IN_PROGRESS,
            CAPA_STATUS_PENDING_VERIFY,
            CAPA_STATUS_CLOSED,
            CAPA_STATUS_CANCELLED
    );

    private final NcrRecordMapper ncrRecordMapper;
    private final ProductionBatchMapper productionBatchMapper;

    public CapaRecordServiceImpl(
            NcrRecordMapper ncrRecordMapper,
            ProductionBatchMapper productionBatchMapper) {
        this.ncrRecordMapper = ncrRecordMapper;
        this.productionBatchMapper = productionBatchMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CapaRecordVO createCapa(CapaCreateDTO dto) {
        validateCapaNoUnique(dto.getCapaNo());
        validateOwnerExists(dto.getOwnerId());

        NcrRecord ncrRecord = getExistingNcrRecord(dto.getNcrId());
        if (!NCR_STATUS_OPEN.equals(ncrRecord.getStatus())) {
            throw new BizException(400, "only OPEN NCR can create CAPA");
        }
        validateNcrNotLinkedToCapa(dto.getNcrId());
        getExistingProductionBatch(ncrRecord.getBatchId());

        CapaRecord capaRecord = new CapaRecord();
        capaRecord.setCapaNo(dto.getCapaNo());
        capaRecord.setNcrId(dto.getNcrId());
        capaRecord.setBatchId(ncrRecord.getBatchId());
        capaRecord.setOwnerId(dto.getOwnerId());
        capaRecord.setRootCause(dto.getRootCause());
        capaRecord.setCorrectiveAction(dto.getCorrectiveAction());
        capaRecord.setPreventiveAction(dto.getPreventiveAction());
        capaRecord.setDueDate(dto.getDueDate());
        capaRecord.setStatus(CAPA_STATUS_IN_PROGRESS);
        save(capaRecord);

        NcrRecord updateNcr = new NcrRecord();
        updateNcr.setId(ncrRecord.getId());
        updateNcr.setStatus(NCR_STATUS_CAPA_CREATED);
        ncrRecordMapper.updateById(updateNcr);

        ProductionBatch updateBatch = new ProductionBatch();
        updateBatch.setId(ncrRecord.getBatchId());
        updateBatch.setStatus(BATCH_STATUS_CAPA_OPEN);
        productionBatchMapper.updateById(updateBatch);

        return toVO(getById(capaRecord.getId()));
    }

    @Override
    public PageResult<CapaRecordVO> pageCapas(
            String capaNo,
            Long ncrId,
            Long batchId,
            Long ownerId,
            String status,
            Long pageNo,
            Long pageSize) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }

        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(capaNo), CapaRecord::getCapaNo, capaNo)
                .eq(ncrId != null, CapaRecord::getNcrId, ncrId)
                .eq(batchId != null, CapaRecord::getBatchId, batchId)
                .eq(ownerId != null, CapaRecord::getOwnerId, ownerId)
                .eq(StringUtils.hasText(status), CapaRecord::getStatus, status)
                .orderByDesc(CapaRecord::getCreatedTime)
                .orderByDesc(CapaRecord::getId);

        Page<CapaRecord> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<CapaRecordVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public CapaRecordVO getCapaDetail(Long id) {
        return toVO(getExistingCapaRecord(id));
    }

    @Override
    public CapaRecordVO getCapaByNcr(Long ncrId) {
        getExistingNcrRecord(ncrId);

        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getNcrId, ncrId);
        CapaRecord capaRecord = getOne(queryWrapper);
        return capaRecord == null ? null : toVO(capaRecord);
    }

    @Override
    public CapaRecordVO updateCapa(Long id, CapaUpdateDTO dto) {
        CapaRecord capaRecord = getExistingCapaRecord(id);
        if (isTerminalStatus(capaRecord.getStatus())) {
            throw new BizException(400, "terminal status CAPA cannot be modified");
        }

        CapaRecord updateEntity = new CapaRecord();
        updateEntity.setId(id);
        if (dto.getRootCause() != null) {
            updateEntity.setRootCause(dto.getRootCause());
        }
        if (dto.getCorrectiveAction() != null) {
            updateEntity.setCorrectiveAction(dto.getCorrectiveAction());
        }
        if (dto.getPreventiveAction() != null) {
            updateEntity.setPreventiveAction(dto.getPreventiveAction());
        }
        if (dto.getVerifyResult() != null) {
            updateEntity.setVerifyResult(dto.getVerifyResult());
        }
        if (dto.getDueDate() != null) {
            updateEntity.setDueDate(dto.getDueDate());
        }

        updateById(updateEntity);
        return toVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CapaRecordVO updateCapaStatus(Long id, CapaStatusUpdateDTO dto) {
        CapaRecord capaRecord = getExistingCapaRecord(id);
        String targetStatus = dto.getStatus();
        validateStatus(targetStatus);

        if (isTerminalStatus(capaRecord.getStatus()) && !capaRecord.getStatus().equals(targetStatus)) {
            throw new BizException(400, "terminal status CAPA cannot switch to another status");
        }

        LocalDateTime now = LocalDateTime.now();
        CapaRecord updateCapa = new CapaRecord();
        updateCapa.setId(id);
        updateCapa.setStatus(targetStatus);
        if (!CAPA_STATUS_CLOSED.equals(capaRecord.getStatus()) && CAPA_STATUS_CLOSED.equals(targetStatus)) {
            updateCapa.setClosedTime(now);
        }
        updateById(updateCapa);

        if (CAPA_STATUS_CLOSED.equals(targetStatus)) {
            closeNcrAndBatch(capaRecord, now);
        }

        return toVO(getById(id));
    }

    private void closeNcrAndBatch(CapaRecord capaRecord, LocalDateTime closedTime) {
        NcrRecord ncrRecord = getExistingNcrRecord(capaRecord.getNcrId());
        getExistingProductionBatch(capaRecord.getBatchId());

        NcrRecord updateNcr = new NcrRecord();
        updateNcr.setId(ncrRecord.getId());
        updateNcr.setStatus(NCR_STATUS_CLOSED);
        updateNcr.setClosedTime(closedTime);
        ncrRecordMapper.updateById(updateNcr);

        ProductionBatch updateBatch = new ProductionBatch();
        updateBatch.setId(capaRecord.getBatchId());
        updateBatch.setStatus(BATCH_STATUS_CLOSED);
        productionBatchMapper.updateById(updateBatch);
    }

    private void validateCapaNoUnique(String capaNo) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getCapaNo, capaNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "capaNo already exists");
        }
    }

    private void validateOwnerExists(Long ownerId) {
        Long count = baseMapper.countSysUserById(ownerId);
        if (count == null || count == 0) {
            throw new BizException(404, "owner user not found");
        }
    }

    private void validateNcrNotLinkedToCapa(Long ncrId) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getNcrId, ncrId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "NCR already has CAPA");
        }
    }

    private CapaRecord getExistingCapaRecord(Long id) {
        CapaRecord capaRecord = getById(id);
        if (capaRecord == null) {
            throw new BizException(404, "CAPA record not found");
        }
        return capaRecord;
    }

    private NcrRecord getExistingNcrRecord(Long ncrId) {
        NcrRecord ncrRecord = ncrRecordMapper.selectById(ncrId);
        if (ncrRecord == null) {
            throw new BizException(404, "NCR record not found");
        }
        return ncrRecord;
    }

    private ProductionBatch getExistingProductionBatch(Long batchId) {
        ProductionBatch productionBatch = productionBatchMapper.selectById(batchId);
        if (productionBatch == null) {
            throw new BizException(404, "production batch not found");
        }
        return productionBatch;
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid CAPA status");
        }
    }

    private boolean isTerminalStatus(String status) {
        return CAPA_STATUS_CLOSED.equals(status) || CAPA_STATUS_CANCELLED.equals(status);
    }

    private CapaRecordVO toVO(CapaRecord capaRecord) {
        CapaRecordVO vo = new CapaRecordVO();
        BeanUtils.copyProperties(capaRecord, vo);
        return vo;
    }
}
