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

/**
 * 文件职责：
 * 实现 CAPA 整改闭环业务，包括从 OPEN NCR 创建 CAPA、编辑措施、状态推进和关闭联动。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * CapaRecordController。
 *
 * 下游依赖：
 * CapaRecordMapper、NcrRecordMapper、ProductionBatchMapper，最终访问 capa_record、ncr_record、production_batch。
 *
 * 主要业务链路：
 * NcrListView.vue 或 CapaListView.vue -> capaApi.js -> CapaRecordController
 * -> CapaRecordServiceImpl -> capa_record / ncr_record / production_batch。
 *
 * 注意事项：
 * CAPA 关闭时需要同时关闭 NCR 和生产批次，保证质量问题、整改措施和批次状态一致。
 */
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

    /**
     * 创建 CAPA。
     *
     * 前置条件：
     * capaNo 唯一；ownerId 必须存在于 sys_user；
     * ncr_record 必须存在且状态为 OPEN；
     * 同一 NCR 只能对应一条 CAPA。
     *
     * 写入数据：
     * 新增 capa_record，当前实现默认 status=IN_PROGRESS；
     * 更新 ncr_record.status=CAPA_CREATED；
     * 更新 production_batch.status=CAPA_OPEN。
     *
     * 事务说明：
     * 三张表代表同一质量闭环的不同阶段，任一更新失败都应整体回滚。
     */
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

    /**
     * 分页查询 CAPA，供 CapaListView 展示和筛选。
     */
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

    /**
     * 查询 CAPA 详情。
     */
    @Override
    public CapaRecordVO getCapaDetail(Long id) {
        return toVO(getExistingCapaRecord(id));
    }

    /**
     * 根据 NCR 查询 CAPA，用于判断同一 ncr_record 是否已有整改记录。
     */
    @Override
    public CapaRecordVO getCapaByNcr(Long ncrId) {
        getExistingNcrRecord(ncrId);

        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getNcrId, ncrId);
        CapaRecord capaRecord = getOne(queryWrapper);
        return capaRecord == null ? null : toVO(capaRecord);
    }

    /**
     * 编辑 CAPA 内容。
     *
     * 前置条件：
     * CLOSED/CANCELLED 终态 CAPA 不能再修改措施内容。
     *
     * 写入数据：
     * 只更新请求中非 null 的根因、纠正措施、预防措施、验证结果和 dueDate。
     */
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

    /**
     * 更新 CAPA 状态。
     *
     * 状态限制：
     * 目标状态必须合法；CLOSED/CANCELLED 终态不能切换到其他状态。
     *
     * 写入数据：
     * 更新 capa_record.status；首次关闭时写入 capa_record.closed_time。
     * 当目标状态为 CLOSED 时，同步关闭 ncr_record 并把 production_batch 置为 CLOSED。
     *
     * 事务说明：
     * 关闭 CAPA 是闭环终点，三张表必须同时成功；中间一步失败会整体回滚。
     */
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

    /**
     * 关闭 CAPA 后联动关闭 NCR 和批次。
     *
     * 状态变化：
     * ncr_record.status -> CLOSED，并写入 closed_time；
     * production_batch.status -> CLOSED；
     * closedTime 由 updateCapaStatus 统一生成，保证 CAPA 和 NCR 关闭时间一致。
     */
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

    /**
     * 校验 CAPA 编号唯一性，对应 capa_record.uk_capa_record_capa_no。
     */
    private void validateCapaNoUnique(String capaNo) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getCapaNo, capaNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "capaNo already exists");
        }
    }

    /**
     * 校验负责人存在性；当前通过 Mapper 自定义 SQL 查询 sys_user。
     */
    private void validateOwnerExists(Long ownerId) {
        Long count = baseMapper.countSysUserById(ownerId);
        if (count == null || count == 0) {
            throw new BizException(404, "owner user not found");
        }
    }

    /**
     * 防止同一 NCR 重复创建 CAPA，对应 capa_record.uk_capa_ncr_id。
     */
    private void validateNcrNotLinkedToCapa(Long ncrId) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getNcrId, ncrId);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "NCR already has CAPA");
        }
    }

    /**
     * 获取已存在 CAPA。
     */
    private CapaRecord getExistingCapaRecord(Long id) {
        CapaRecord capaRecord = getById(id);
        if (capaRecord == null) {
            throw new BizException(404, "CAPA record not found");
        }
        return capaRecord;
    }

    /**
     * 获取已存在 NCR，CAPA 创建和关闭都需要依赖它。
     */
    private NcrRecord getExistingNcrRecord(Long ncrId) {
        NcrRecord ncrRecord = ncrRecordMapper.selectById(ncrId);
        if (ncrRecord == null) {
            throw new BizException(404, "NCR record not found");
        }
        return ncrRecord;
    }

    /**
     * 获取已存在批次，确保 CAPA_OPEN/CLOSED 状态同步有真实目标。
     */
    private ProductionBatch getExistingProductionBatch(Long batchId) {
        ProductionBatch productionBatch = productionBatchMapper.selectById(batchId);
        if (productionBatch == null) {
            throw new BizException(404, "production batch not found");
        }
        return productionBatch;
    }

    /**
     * 校验 CAPA 状态枚举。
     */
    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid CAPA status");
        }
    }

    /**
     * 判断 CAPA 是否处于不可逆终态。
     */
    private boolean isTerminalStatus(String status) {
        return CAPA_STATUS_CLOSED.equals(status) || CAPA_STATUS_CANCELLED.equals(status);
    }

    /**
     * Entity 转 VO，供 Controller 返回给前端。
     */
    private CapaRecordVO toVO(CapaRecord capaRecord) {
        CapaRecordVO vo = new CapaRecordVO();
        BeanUtils.copyProperties(capaRecord, vo);
        return vo;
    }
}
