package com.example.visualqms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.CapaRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CapaRecordMapper extends BaseMapper<CapaRecord> {

    @Select("SELECT COUNT(1) FROM sys_user WHERE id = #{userId}")
    Long countSysUserById(@Param("userId") Long userId);
}
