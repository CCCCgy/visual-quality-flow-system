package com.example.visualqms.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件职责：
 * 注册 MyBatis-Plus 的分页拦截器。
 *
 * 所属层级：
 * Config。
 *
 * 上游调用：
 * Spring Boot 启动时加载本配置类并创建 MybatisPlusInterceptor Bean。
 *
 * 下游依赖：
 * 各 ServiceImpl 调用 page(new Page<>(...)) 时依赖该拦截器生成 MySQL 分页 SQL。
 *
 * 注意事项：
 * 这里仅配置分页能力，不改变 Mapper 对表的映射关系。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 为 MySQL 方言启用分页拦截，支撑列表页的 pageNo/pageSize 查询。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
