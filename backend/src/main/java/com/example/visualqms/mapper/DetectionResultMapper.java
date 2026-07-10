package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.DetectionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件职责：
 * detection_result 表的数据访问入口。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * DetectionImportServiceImpl 写入检测框；DetectionResultServiceImpl 查询；
 * ReviewRecordServiceImpl 同步复核结论；NcrRecordServiceImpl 追溯确认缺陷；DashboardServiceImpl 统计状态和类别。
 *
 * 下游依赖：
 * MyBatis-Plus BaseMapper 提供基础 CRUD、分页和 group by 统计能力。
 */
@Mapper
public interface DetectionResultMapper extends BaseMapper<DetectionResult> {
}
