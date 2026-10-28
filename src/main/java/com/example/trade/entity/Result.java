package com.example.trade.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体。
 *
 * code 的语义 = HTTP 状态码，成功是 0：
 *   0    成功
 *   400  业务失败 / 参数校验失败（message 是给人看的原因）
 *   401  未登录 / token 失效
 *   404  接口不存在
 *   405  请求方式不对
 *   500  服务端异常
 *
 * 设计原则：**code 和 HTTP 状态码保持一致**。
 * 之前是混着用的 —— Controller 里 return Result.error() 走 HTTP 200，
 * 抛 BusinessException 走 HTTP 400，同样的错误两种状态码，
 * 前端没法统一判断。现在统一由 GlobalExceptionHandler 决定 HTTP 状态码。
 *
 * 另外修了一处：原来的 error() 把原因塞进了 data 字段，message 恒为"操作失败"，
 * 前端读 message 拿不到真实原因。已统一为 message 放原因、data 放业务数据。
 *
 * @author maofan
 * @create 2026-10-25 21:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <E> Result<E> success(E data) {
        return new Result<>(0, "操作成功", data);
    }

    public static Result success() {
        return new Result<>(0, "操作成功", null);
    }

    /**
     * 业务失败。
     *
     * ⚠️ 业务层请改用 throw new BusinessException("原因")，
     *    不要直接 return Result.error(...)。
     *    因为直接 return 的默认走 HTTP 200，和异常处理器的 400 不一致。
     *    这个方法保留给异常处理器内部使用。
     */
    public static Result error(Integer code, String message) {
        return new Result<>(code, message, (Object) null);
    }
}
