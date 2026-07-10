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

/**
 * 文件职责：
 * 实现检测任务业务规则，包括任务编号唯一性、批次存在性校验、关闭批次禁止创建任务和任务状态管理。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * InspectionTaskController。
 *
 * 下游依赖：
 * 通过 InspectionTaskMapper 访问 inspection_task，通过 ProductionBatchMapper 校验 production_batch。
 *
 * 主要业务链路：
 * BatchDetailView.vue 或 InspectionTaskListView.vue -> taskApi.js -> InspectionTaskController
 * -> InspectionTaskServiceImpl -> InspectionTaskMapper / ProductionBatchMapper -> inspection_task / production_batch。
 *
 * 注意事项：
 * 创建任务时只建立任务与批次关系，YOLO JSON 导入和检测结果落库由 DetectionImportServiceImpl 完成。
 */
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

    /**
     * 创建检测任务。
     *
     * 前置条件：
     * taskNo 唯一；batchId 必须指向存在的 production_batch；CLOSED 批次不能再创建检测任务。
     *
     * 写入数据：
     * 新增 inspection_task，默认 status=CREATED；sourceType 为空时使用 YOLO_JSON。
     *
     * @return 创建后的任务 VO
     */
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

    /**
     * 分页查询检测任务。
     *
     * 查询数据：
     * inspection_task，可按任务号、批次 ID 和任务状态过滤。
     */
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

    /**
     * 查询任务详情。
     */
    @Override
    public InspectionTaskVO getTaskDetail(Long id) {
        return toVO(getExistingTask(id));
    }

    /**
     * 查询某批次下的检测任务。
     *
     * 前置条件：
     * 先校验 production_batch 存在，避免前端在不存在批次详情页看到空列表而误判为无任务。
     */
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

    /**
     * 更新任务状态。
     *
     * 状态限制：
     * 目标状态必须合法；CLOSED/CANCELLED 是终态，不能再切换到其他状态。
     */
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

    /**
     * 校验任务编号唯一性，对应 inspection_task.uk_inspection_task_task_no。
     */
    private void validateTaskNoUnique(String taskNo) {
        LambdaQueryWrapper<InspectionTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InspectionTask::getTaskNo, taskNo);
        if (count(queryWrapper) > 0) {
            throw new BizException(400, "taskNo already exists");
        }
    }

    /**
     * 获取已存在任务，不存在时抛出业务异常。
     */
    private InspectionTask getExistingTask(Long id) {
        InspectionTask task = getById(id);
        if (task == null) {
            throw new BizException(404, "inspection task not found");
        }
        return task;
    }

    /**
     * 获取已存在批次，用于建立 inspection_task.batch_id 与 production_batch.id 的业务关联。
     */
    private ProductionBatch getExistingBatch(Long batchId) {
        ProductionBatch batch = productionBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException(404, "production batch not found");
        }
        return batch;
    }

    /**
     * 校验任务状态枚举。
     */
    private void validateStatus(String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(400, "invalid inspection task status");
        }
    }

    /**
     * 判断任务是否已进入不可逆终态。
     */
    private boolean isTerminalStatus(String status) {
        return STATUS_CLOSED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    /**
     * Entity 转 VO，保持接口返回结构与数据库对象解耦。
     */
    private InspectionTaskVO toVO(InspectionTask task) {
        InspectionTaskVO vo = new InspectionTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
