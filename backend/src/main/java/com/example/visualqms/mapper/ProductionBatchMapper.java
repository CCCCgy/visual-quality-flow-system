package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.ProductionBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件职责：
 * production_batch 表的数据访问入口。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * ProductionBatchServiceImpl、InspectionTaskServiceImpl、NcrRecordServiceImpl、CapaRecordServiceImpl、DashboardServiceImpl。
 *
 * 下游依赖：
 * MyBatis-Plus BaseMapper 提供 selectById、insert、updateById、selectCount、selectMaps 等基础能力。
 *
 * 注意事项：
 * 本 Mapper 没有自定义 SQL；批次状态业务规则在 ServiceImpl 中维护。
 */
@Mapper
public interface ProductionBatchMapper extends BaseMapper<ProductionBatch> {
}
