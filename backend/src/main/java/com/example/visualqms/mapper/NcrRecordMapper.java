package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.NcrRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件职责：
 * ncr_record 表的数据访问入口，并提供 NCR 创建人存在性校验 SQL。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * NcrRecordServiceImpl 创建/查询/更新 NCR；CapaRecordServiceImpl 创建和关闭 CAPA 时同步 NCR；
 * DashboardServiceImpl 统计 NCR 状态。
 *
 * 下游依赖：
 * BaseMapper 访问 ncr_record；countSysUserById 只读 sys_user。
 */
@Mapper
public interface NcrRecordMapper extends BaseMapper<NcrRecord> {

    /**
     * 查询 sys_user 是否存在指定创建人。
     *
     * @param userId 创建人用户 ID
     * @return 匹配用户数量，0 表示不存在
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE id = #{userId}")
    Long countSysUserById(@Param("userId") Long userId);
}
