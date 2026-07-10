package com.example.visualqms.controller;

import com.example.visualqms.common.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供后端健康检查接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * 浏览器、接口调试工具、部署检查脚本或前端开发者可以访问 GET /api/health。
 *
 * 下游依赖：
 * 不访问数据库，只返回服务是否已被 Spring MVC 正常接管。
 *
 * 注意事项：
 * 该接口用于确认应用进程可响应请求，不代表 MySQL 或完整业务链路可用。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 返回服务存活状态。
     *
     * @return status=UP 和服务名，外层由 Result 包装
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
                "status", "UP",
                "service", "visual-quality-flow-system"
        ));
    }
}
