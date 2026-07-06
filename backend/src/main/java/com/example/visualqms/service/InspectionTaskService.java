package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.InspectionTaskCreateDTO;
import com.example.visualqms.dto.InspectionTaskStatusUpdateDTO;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.vo.InspectionTaskVO;
import java.util.List;

public interface InspectionTaskService extends IService<InspectionTask> {

    InspectionTaskVO createTask(InspectionTaskCreateDTO dto);

    PageResult<InspectionTaskVO> pageTasks(String taskNo, Long batchId, String status, Long pageNo, Long pageSize);

    InspectionTaskVO getTaskDetail(Long id);

    List<InspectionTaskVO> listTasksByBatch(Long batchId);

    InspectionTaskVO updateTaskStatus(Long id, InspectionTaskStatusUpdateDTO dto);
}
