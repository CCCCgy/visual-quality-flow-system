package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.CapaRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件职责：
 * capa_record 表的数据访问入口，并提供 CAPA 负责人存在性校验 SQL。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * CapaRecordServiceImpl 创建/编辑/查询/关闭 CAPA；DashboardServiceImpl 统计 CAPA 状态。
 *
 * 下游依赖：
 * BaseMapper 访问 capa_record；countSysUserById 只读 sys_user。
 *
 * 注意事项：
 * 关闭 CAPA 后同步 NCR 和批次的动作在 ServiceImpl 中完成，不在 Mapper 中隐藏业务逻辑。
 */
@Mapper
public interface CapaRecordMapper extends BaseMapper<CapaRecord> {

    /**
     * 查询 sys_user 是否存在指定负责人。
     *
     * @param userId CAPA 负责人用户 ID
     * @return 匹配用户数量，0 表示不存在
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE id = #{userId}")
    Long countSysUserById(@Param("userId") Long userId);
}
