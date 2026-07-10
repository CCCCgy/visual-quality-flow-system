package com.example.visualqms.exception;

import lombok.Getter;

/**
 * 文件职责：
 * 表示可预期、可展示给前端的业务异常。
 *
 * 所属层级：
 * exception。
 *
 * 上游调用：
 * ServiceImpl 在发现批次号重复、状态不允许、关联数据不存在等业务问题时抛出。
 *
 * 下游依赖：
 * GlobalExceptionHandler 捕获后转换为统一 Result，前端 request.js 使用 message 展示错误。
 *
 * 注意事项：
 * 业务异常不同于系统异常；它通常说明用户操作不满足业务前置条件，而不是程序崩溃。
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
