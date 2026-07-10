package com.example.visualqms.exception;

import com.example.visualqms.common.Result;
import java.util.stream.Collectors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 文件职责：
 * 统一捕获 Controller 调用链中抛出的异常，并转换为 Result 响应。
 *
 * 所属层级：
 * exception。
 *
 * 上游调用：
 * Spring MVC 在 Controller、参数校验、Service 调用出现异常时自动进入本处理器。
 *
 * 下游依赖：
 * 返回 Result.fail 给前端 request.js，保持错误响应结构与成功响应一致。
 *
 * 注意事项：
 * BizException 会保留业务错误码和信息；未知 Exception 只返回通用信息，避免泄露服务端细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 Service 主动抛出的业务异常，例如状态流转非法或关联记录不存在。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理 @Valid 触发的请求体字段校验错误，通常对应 DTO 上的 NotBlank/NotNull/Min。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(400, message);
    }

    /**
     * 兜底处理未预期异常，使前端仍收到统一 JSON 结构。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail("Internal server error");
    }

    /**
     * 将单个字段错误格式化为前端可读的短消息。
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
