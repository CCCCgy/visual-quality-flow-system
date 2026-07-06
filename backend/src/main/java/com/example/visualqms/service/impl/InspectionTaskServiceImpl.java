package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.InspectionTaskCreateDTO;
import com.example.visualqms.dto.InspectionTaskStatusUpdateDTO;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.exception.BizException;
import com.example.visualqms.mapper.InspectionTaskMapper;
import com.example.visualqms.mapper.ProductionBatchMapper;
import com.example.visualqms.service.InspectionTaskService;
import com.example.visualqms.vo.InspectionTaskVO;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InspectionTaskServiceImpl
        extends ServiceImpl<InspectionTaskMapper, InspectionTask>
        implements InspectionTaskService {

    private static final String DEFAULT_SOURCE_TYPE = "YOLO_JSON";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> ALLOWED_STATUS = Set.of(
            "CREATED",
            "WAIT_REVIEW",
            "REVIEWED",
            "CLOSED",
            "CANCELLED"
    );

    private final ProductionBatchMapper productionBatchMapper;

    public InspectionTaskServiceImpl(ProductionBatchMapper productionBatchMapper) {
        this.productionBatchMapper = productionBatchMapper;
    }

    @Override
    public InspectionTaskVO createTask(InspectionTaskCreateDTO dto) {
        validateTaskNoUnique(dto.getTaskNo());
        ProductionBatch batch = getExistingBatch(dto.getBatchId());
        if (STATUS_CLOSED.equals(batch.getStatus())) {
            throw new BizException("CLOSED status batch cannot create inspection task");
        }

        InspectionTask task = new InspectionTask();
        task.setTaskNo(dto.getTaskNo());
        task.setBatchId(dto.getBatchId());
        task.setModelName(dto.getModelName());
        task.setModelVersion(dto.getModelVersion());
        task.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : DEFAULT_SOURCE_TYPE);
        task.setCreatedBy(dto.getCreatedBy());
        task.setStatus(STATUS_CREATED);

        save(task);
        return toVO(getById(task.getId()));
    }

    @Override
    public PageResult<InspectionTaskVO> pageTasks(String taskNo, Long batchId, String status, Long pageNo, Long pageSize) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }

        LambdaQueryWrapper<InspectionTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(taskNo), InspectionTask::getTaskNo, taskNo)
                .eq(batchId != null, InspectionTask::getBatchId, batchId)
                .eq(StringUtils.hasText(status), InspectionTask::getStatus, status)
                .orderByDesc(InspectionTask::getCreatedTime)
                .orderByDesc(InspectionTask::getId);

        Page<InspectionTask> page = page(new Page<>(pageNo, pageSize), queryWrapper);
        List<InspectionTaskVO> records = page.getRecords()
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public InspectionTaskVO getTaskDetail(Long id) {
        return toVO(getExistingTask(id));
    }

    @Override
    public List<InspectionTaskVO> listTasksByBatch(Long batchId) {
        getExistingBatch(batchId);

        LambdaQueryWrapper<InspectionTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InspectionTask::getBatchId, batchId)
                .orderByDesc(InspectionTask::getCreatedTime)
                .orderByDesc(InspectionTask::getId);
        return list(queryWrapper)
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public InspectionTaskVO updateTaskStatus(Long id, InspectionTaskStatusUpdateDTO dto) {
        InspectionTask task = getExistingTask(id);
        String targetStatus = dto.getStatus();
        validateStatus(targetStatus);

        if (isTerminalStatus(task.getStatus()) && !task.getStatus().equals(targetStatus)) {
            throw new BizException("terminal status inspection task cannot switch to another status");
        }

        InspectionTask updateEntity = new InspectionTask();
        updateEntity.setId(id);
        updateEntity.setStatus(targetStatus);
        updateById(updateEntity);
        return toVO(getById(id));
    }

    private void validateTaskNoUnique(String taskNo) {
        LambdaQueryWrapper<InspectionTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InspectionTask::getTaskNo, taskNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "taskNo already exists");
        }
    }

    private InspectionTask getExistingTask(Long id) {
        InspectionTask task = getById(id);
        if (task == null) {
            throw new BizException(404, "inspection task not found");
        }
        return task;
    }

    private ProductionBatch getExistingBatch(Long batchId) {
        ProductionBatch batch = productionBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(404, "production batch not found");
        }
        return batch;
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid inspection task status");
        }
    }

    private boolean isTerminalStatus(String status) {
        return STATUS_CLOSED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    private InspectionTaskVO toVO(InspectionTask task) {
        InspectionTaskVO vo = new InspectionTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
