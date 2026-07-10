package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件职责：
 * review_record 表的数据访问入口，并提供复核人存在性校验 SQL。
 *
 * 所属层级：
 * Mapper。
 *
 * 上游调用：
 * ReviewRecordServiceImpl 创建/查询复核记录；NcrRecordServiceImpl 创建 NCR 时读取复核来源。
 *
 * 下游依赖：
 * BaseMapper 访问 review_record；countSysUserById 只读 sys_user，用于校验 reviewerId。
 *
 * 注意事项：
 * 这里不负责写 detection_result.status，同步状态由 ReviewRecordServiceImpl 的事务方法完成。
 */
@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {

    /**
     * 查询 sys_user 是否存在指定用户。
     *
     * @param userId 复核人用户 ID
     * @return 匹配用户数量，0 表示不存在
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE id = #{userId}")
    Long countSysUserById(@Param("userId") Long userId);
}
