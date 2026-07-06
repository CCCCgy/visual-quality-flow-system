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

    @Override
    public ProductionBatchVO getBatchDetail(Long id) {
        return toVO(getExistingBatch(id));
    }

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

    private void validateBatchNoUnique(String batchNo) {
        LambdaQueryWrapper<ProductionBatch> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductionBatch::getBatchNo, batchNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "batchNo already exists");
        }
    }

    private ProductionBatch getExistingBatch(Long id) {
        ProductionBatch batch = getById(id);
        if (batch == null) {
            throw new BizException(404, "production batch not found");
        }
        return batch;
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid production batch status");
        }
    }

    private ProductionBatchVO toVO(ProductionBatch batch) {
        ProductionBatchVO vo = new ProductionBatchVO();
        BeanUtils.copyProperties(batch, vo);
        return vo;
    }
}
