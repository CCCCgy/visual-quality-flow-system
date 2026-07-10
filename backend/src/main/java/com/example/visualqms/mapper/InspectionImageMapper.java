package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.InspectionImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件职责：
 * inspection_image 表的数据访问入口。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * DetectionImportServiceImpl 创建或更新图片记录；DetectionResultServiceImpl 查询视觉详情。
 *
 * 下游依赖：
 * MyBatis-Plus BaseMapper 提供基础 CRUD。
 *
 * 注意事项：
 * 图片记录由 YOLO JSON 的 sourceName 生成或复用，不在 Mapper 中写自定义关联 SQL。
 */
@Mapper
public interface InspectionImageMapper extends BaseMapper<InspectionImage> {
}
