package com.example.visualqms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件职责：
 * Spring Boot 后端启动入口，负责拉起 Visual QMS 的 Controller、Service、Mapper 等 Bean。
 *
 * 所属层级：
 * Application。
 *
 * 上游调用：
 * 由本地命令、IDE、Maven 插件或打包后的 Java 进程调用 main 方法启动。
 *
 * 下游依赖：
 * {@link SpringBootApplication} 会从 com.example.visualqms 包向下扫描组件，
 * 因而发现 controller、service.impl、config、mapper 等包中的 Spring Bean。
 *
 * 主要业务链路：
 * 前端 Vue 页面 -> Axios /api 请求 -> Spring MVC Controller -> Service -> Mapper -> MySQL 表。
 *
 * 注意事项：
 * 实际监听端口来自 application.yml 的 server.port；当前配置为 8080。
 */
@SpringBootApplication
public class VisualQmsApplication {

    /**
     * 启动 Spring 应用上下文，使前端经 Vite 代理或浏览器直连发出的 HTTP 请求能够进入 Controller。
     *
     * @param args JVM 启动参数，当前项目未在业务逻辑中直接使用
     */
    public static void main(String[] args) {
        SpringApplication.run(VisualQmsApplication.class, args);
    }
}
