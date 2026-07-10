package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.InspectionTaskCreateDTO;
import com.example.visualqms.dto.InspectionTaskStatusUpdateDTO;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.vo.InspectionTaskVO;
import java.util.List;

/**
 * 文件职责：
 * 定义检测任务模块的业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * InspectionTaskController。
 *
 * 下游依赖：
 * 由 InspectionTaskServiceImpl 实现，访问 inspection_task 并校验 production_batch。
 *
 * 设计说明：
 * Service 负责业务规则，Mapper 只负责表访问；因此 Controller 通过接口调用业务动作而不直接操作表。
 */
public interface InspectionTaskService extends IService<InspectionTask> {

    /** 创建检测任务，对应 POST /api/inspection-tasks。 */
    InspectionTaskVO createTask(InspectionTaskCreateDTO dto);

    /** 分页查询检测任务，对应 GET /api/inspection-tasks。 */
    PageResult<InspectionTaskVO> pageTasks(String taskNo, Long batchId, String status, Long pageNo, Long pageSize);

    /** 查询检测任务详情，对应 GET /api/inspection-tasks/{id}。 */
    InspectionTaskVO getTaskDetail(Long id);

    /** 查询某批次下全部检测任务，对应 GET /api/inspection-tasks/by-batch/{batchId}。 */
    List<InspectionTaskVO> listTasksByBatch(Long batchId);

    /** 更新任务状态，对应 PATCH /api/inspection-tasks/{id}/status。 */
    InspectionTaskVO updateTaskStatus(Long id, InspectionTaskStatusUpdateDTO dto);
}
