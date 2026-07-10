package com.example.visualqms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件职责：
 * 配置 OpenAPI/Knife4j 接口文档元信息和扫描范围。
 *
 * 所属层级：
 * Config。
 *
 * 上游调用：
 * Spring Boot 启动时加载本配置类。
 *
 * 下游依赖：
 * Knife4j 根据 com.example.visualqms.controller 包中的 Controller 生成接口文档。
 *
 * 注意事项：
 * 该配置只影响接口文档展示，不改变接口路径、入参或返回结构。
 */
@Configuration
public class Knife4jConfig {

    /**
     * 定义接口文档标题、描述和版本。
     */
    @Bean
    public OpenAPI visualQmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Visual Quality Flow System API")
                        .description("Industrial visual inspection review and quality closed-loop system backend API")
                        .version("0.0.1"));
    }

    /**
     * 指定只扫描 Controller 包，避免把非接口类暴露进文档分组。
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.example.visualqms.controller")
                .build();
    }
}
