package com.example.visualqms.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件职责：
 * 定义后端所有接口的统一响应结构。
 *
 * 所属层级：
 * common。
 *
 * 上游调用：
 * 由各 Controller 和 GlobalExceptionHandler 构造成功或失败响应。
 *
 * 下游依赖：
 * 前端 request.js 在响应拦截器中读取 code、message、data；code=200 时返回 data，
 * 非 200 时展示 message 并抛出错误。
 *
 * 注意事项：
 * 该结构是前后端契约，修改字段名或成功码会影响所有 api/*.js 调用方。
 *
 * @param <T> data 字段承载的业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;
    public static final String SUCCESS_MESSAGE = "success";

    private Integer code;
    private String message;
    private T data;

    /**
     * 构造无业务数据的成功响应，通常用于健康检查或只需要确认操作完成的接口。
     */
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 构造带业务数据的成功响应，Controller 返回 VO、分页结果或统计列表时使用。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 构造默认 500 错误响应，通常由系统异常兜底处理使用。
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(FAIL_CODE, message, null);
    }

    /**
     * 构造指定错误码的失败响应，业务校验失败和参数校验失败会使用更具体的 code。
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
