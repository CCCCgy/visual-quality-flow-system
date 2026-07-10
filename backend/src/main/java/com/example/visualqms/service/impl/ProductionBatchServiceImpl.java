package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ProductionBatchCreateDTO;
import com.example.visualqms.dto.ProductionBatchStatusUpdateDTO;
import com.example.visualqms.dto.ProductionBatchUpdateDTO;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.ProductionBatchMapper;
import com.example.visualqms.service.ProductionBatchService;
import com.example.visualqms.vo.ProductionBatchVO;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件职责：
 * 实现生产批次的业务规则，包括批次编号唯一性、分页查询、详情、非状态字段更新和状态更新。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * ProductionBatchController。
 *
 * 下游依赖：
 * 继承 MyBatis-Plus ServiceImpl，内部通过 ProductionBatchMapper 访问 production_batch 表。
 *
 * 主要业务链路：
 * BatchListView.vue -> batchApi.js -> ProductionBatchController -> ProductionBatchService
 * -> ProductionBatchServiceImpl -> ProductionBatchMapper -> production_batch。
 *
 * 注意事项：
 * 批次状态也会被 NCR/CAPA 流程跨服务更新；本类只处理批次自身接口发起的状态限制。
 */
@Service
public class ProductionBatchServiceImpl
        extends ServiceImpl<ProductionBatchMapper, ProductionBatch>
        implements ProductionBatchService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final Set<String> ALLOWED_STATUS = Set.of(
            "CREATED",
            "INSPECTING",
            "NCR_OPEN",
            "CAPA_OPEN",
            "CLOSED"
    );

    /**
     * 创建生产批次。
     *
     * 前置条件：
     * batchNo 不能与 production_batch 中已有记录重复。
     *
     * 写入数据：
     * 新增 production_batch，默认 status=CREATED。
     *
     * 状态变化：
     * 无旧状态，新批次从 CREATED 开始，后续可由检测、NCR、CAPA 链路推进。
     *
     * @param dto 批次创建参数
     * @return 创建后的 VO，重新查询是为了带出数据库生成的 id 和时间字段
     */
    @Override
    public ProductionBatchVO createBatch(ProductionBatchCreateDTO dto) {
        validateBatchNoUnique(dto.getBatchNo());

        ProductionBatch batch = new ProductionBatch();
        batch.setBatchNo(dto.getBatchNo());
        batch.setProductCode(dto.getProductCode());
        batch.setProductName(dto.getProductName());
        batch.setPlannedQuantity(dto.getPlannedQuantity());
        batch.setCreatedBy(dto.getCreatedBy());
        batch.setRemark(dto.getRemark());
        batch.setStatus(STATUS_CREATED);

        save(batch);
        return toVO(getById(batch.getId()));
    }

    /**
     * 分页查询批次列表。
     *
     * 查询数据：
     * 访问 production_batch，并按 batchNo 模糊匹配、status 精确过滤。
     *
     * @return PageResult 供前端 el-table 与 el-pagination 使用
     */
    @Override
    public PageResult<ProductionBatchVO> pageBatches(String batchNo, String status, Long pageNo, Long pageSize) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }

        LambdaQueryWrapper<ProductionBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(batchNo), ProductionBatch::getBatchNo, batchNo)
                .eq(StringUtils.hasText(status), ProductionBatch::getStatus, status)
                .orderByDesc(ProductionBatch::getCreatedTime)
                .orderByDesc(ProductionBatch::getId);

        Page<ProductionBatch> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<ProductionBatchVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询批次详情。
     *
     * 查询数据：
     * 根据 production_batch.id 获取单条批次，不存在时抛出 BizException。
     */
    @Override
    public ProductionBatchVO getBatchDetail(Long id) {
        return toVO(getExistingBatch(id));
    }

    /**
     * 更新批次非状态字段。
     *
     * 前置条件：
     * CLOSED 批次不可修改，避免质量闭环结束后再改变批次基础信息。
     *
     * 写入数据：
     * 仅更新调用方明确传入的字段，不触碰 batchNo、status 和时间字段。
     */
    @Override
    public ProductionBatchVO updateBatch(Long id, ProductionBatchUpdateDTO dto) {
        ProductionBatch batch = getExistingBatch(id);
        if (STATUS_CLOSED.equals(batch.getStatus())) {
            throw new BizException("CLOSED status batch cannot be modified");
        }

        ProductionBatch updateEntity = new ProductionBatch();
        updateEntity.setId(id);

        if (dto.getProductCode() != null) {
            if (!StringUtils.hasText(dto.getProductCode())) {
                throw new BizException(400, "productCode cannot be blank");
            }
            updateEntity.setProductCode(dto.getProductCode());
        }
        if (dto.getProductName() != null) {
            if (!StringUtils.hasText(dto.getProductName())) {
                throw new BizException(400, "productName cannot be blank");
            }
            updateEntity.setProductName(dto.getProductName());
        }
        if (dto.getPlannedQuantity() != null) {
            updateEntity.setPlannedQuantity(dto.getPlannedQuantity());
        }
        if (dto.getRemark() != null) {
            updateEntity.setRemark(dto.getRemark());
        }

        updateById(updateEntity);
        return toVO(getById(id));
    }

    /**
     * 更新批次状态。
     *
     * 状态限制：
     * 目标状态必须在 ALLOWED_STATUS 内；已 CLOSED 的批次不能切换回其他状态。
     *
     * 写入数据：
     * 更新 production_batch.status。
     */
    @Override
    public ProductionBatchVO updateBatchStatus(Long id, ProductionBatchStatusUpdateDTO dto) {
        ProductionBatch batch = getExistingBatch(id);
        String targetStatus = dto.getStatus();
        validateStatus(targetStatus);

        if (STATUS_CLOSED.equals(batch.getStatus()) && !STATUS_CLOSED.equals(targetStatus)) {
            throw new BizException("CLOSED status batch cannot switch to another status");
        }

        ProductionBatch updateEntity = new ProductionBatch();
        updateEntity.setId(id);
        updateEntity.setStatus(targetStatus);
        updateById(updateEntity);
        return toVO(getById(id));
    }

    /**
     * 校验批次编号唯一性，对应 production_batch.uk_production_batch_batch_no。
     */
    private void validateBatchNoUnique(String batchNo) {
        LambdaQueryWrapper<ProductionBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductionBatch::getBatchNo, batchNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "batchNo already exists");
        }
    }

    /**
     * 统一封装批次存在性校验，让调用方不用重复处理空记录。
     */
    private ProductionBatch getExistingBatch(Long id) {
        ProductionBatch batch = getById(id);
        if (batch == null) {
            throw new BizException(404, "production batch not found");
        }
        return batch;
    }

    /**
     * 校验批次状态是否属于系统允许的状态集合。
     */
    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid production batch status");
        }
    }

    /**
     * Entity 转 VO：避免 Controller 直接暴露持久化对象。
     */
    private ProductionBatchVO toVO(ProductionBatch batch) {
        ProductionBatchVO vo = new ProductionBatchVO();
        BeanUtils.copyProperties(batch, vo);
        return vo;
    }
}
