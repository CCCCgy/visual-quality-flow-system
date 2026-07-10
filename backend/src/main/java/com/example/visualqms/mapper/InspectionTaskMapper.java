package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.InspectionTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件职责：
 * inspection_task 表的数据访问入口。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * InspectionTaskServiceImpl、DetectionImportServiceImpl、NcrRecordServiceImpl、DashboardServiceImpl。
 *
 * 下游依赖：
 * MyBatis-Plus BaseMapper 提供检测任务的基础 CRUD 和统计能力。
 *
 * 注意事项：
 * 任务与批次的合法性由 Service 层通过 ProductionBatchMapper 协同校验。
 */
@Mapper
public interface InspectionTaskMapper extends BaseMapper<InspectionTask> {
}
