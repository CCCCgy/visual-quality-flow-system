package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.InspectionTaskCreateDTO;
import com.example.visualqms.dto.InspectionTaskStatusUpdateDTO;
import com.example.visualqms.service.InspectionTaskService;
import com.example.visualqms.vo.InspectionTaskVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供检测任务的创建、分页查询、详情、按批次查询和状态调整接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * 前端 taskApi.js 被 InspectionTaskListView.vue 与 BatchDetailView.vue 使用；创建/状态接口可由接口文档或扩展页面调用。
 *
 * 下游依赖：
 * 调用 InspectionTaskService，由 Service 校验 batchId 是否存在以及 CLOSED 批次不能创建任务。
 *
 * 主要业务链路：
 * BatchDetailView.vue 或 InspectionTaskListView.vue -> taskApi.js -> /api/inspection-tasks
 * -> InspectionTaskController -> InspectionTaskService -> InspectionTaskServiceImpl
 * -> InspectionTaskMapper / ProductionBatchMapper -> inspection_task / production_batch。
 *
 * 注意事项：
 * Controller 不直接操作 Mapper，避免绕过任务编号唯一性、批次状态限制和任务终态限制。
 */
@Validated
@RestController
@RequestMapping("/api/inspection-tasks")
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;

    public InspectionTaskController(InspectionTaskService inspectionTaskService) {
        this.inspectionTaskService = inspectionTaskService;
    }

    /**
     * 创建检测任务。
     *
     * @param dto 前端提交的任务编号、批次 ID、模型信息和创建人
     * @return 创建后的检测任务；默认来源类型为空时由 Service 设为 YOLO_JSON
     */
    @PostMapping
    public Result<InspectionTaskVO> createTask(@Valid @RequestBody InspectionTaskCreateDTO dto) {
        return Result.success(inspectionTaskService.createTask(dto));
    }

    /**
     * 分页查询检测任务。
     *
     * @param taskNo 任务号模糊查询条件
     * @param batchId 所属 production_batch.id 过滤条件
     * @param status 任务状态过滤条件
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 检测任务分页结果
     */
    @GetMapping
    public Result<PageResult<InspectionTaskVO>> pageTasks(
            @RequestParam(required = false) String taskNo,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(inspectionTaskService.pageTasks(taskNo, batchId, status, pageNo, pageSize));
    }

    /**
     * 查询检测任务详情。
     *
     * @param id inspection_task 主键
     * @return 检测任务详情
     */
    @GetMapping("/{id}")
    public Result<InspectionTaskVO> getTaskDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(inspectionTaskService.getTaskDetail(id));
    }

    /**
     * 查询某个批次下的全部检测任务。
     *
     * 调用链：
     * BatchDetailView.vue
     * -> taskApi.js#getInspectionTasksByBatch
     * -> GET /api/inspection-tasks/by-batch/{batchId}
     * -> InspectionTaskService#listTasksByBatch
     * -> inspection_task
     *
     * @param batchId production_batch 主键
     * @return 该批次下的检测任务列表
     */
    @GetMapping("/by-batch/{batchId}")
    public Result<List<InspectionTaskVO>> listTasksByBatch(
            @PathVariable @NotNull(message = "batchId cannot be null") Long batchId) {
        return Result.success(inspectionTaskService.listTasksByBatch(batchId));
    }

    /**
     * 更新检测任务状态。
     *
     * @param id inspection_task 主键
     * @param dto 目标状态；CLOSED/CANCELLED 终态不能切换到其他状态
     * @return 状态更新后的任务信息
     */
    @PatchMapping("/{id}/status")
    public Result<InspectionTaskVO> updateTaskStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody InspectionTaskStatusUpdateDTO dto) {
        return Result.success(inspectionTaskService.updateTaskStatus(id, dto));
    }
}
