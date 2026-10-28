package com.example.trade.except;

import com.example.trade.entity.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 *
 * 为什么要分类型：
 *   1. 业务异常（自己抛的）→ 4xx 语义 + 明确原因，前端能直接弹窗
 *   2. 参数校验异常 → 400 + 哪个字段错了
 *   3. 兜底 Exception → 500 + 模糊提示，真实堆栈只进日志，不外泄
 *
 * 原来的写法只有一个 catch(Exception)，导致"用户不存在"这种业务提示
 * 也走 500 分支，且 HTTP 状态码恒为 200，监控和前端都判断不出来。
 *
 * @author maofan
 * @create 2026-10-25 21:26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：自己主动抛的（商品不存在 / 已售出 / 不能购买自己的商品…）
     * 这类异常代表"用户操作不合理"，不是服务器故障。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * @RequestBody 上的 @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return Result.error(400, message);
    }

    /**
     * 表单/查询参数绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return Result.error(400, message);
    }

    /**
     * 参数类型不匹配（比如 goodsId 传了 "abc"）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(400, "参数格式不正确：" + e.getName());
    }

    /**
     * 请求体解析失败（JSON 格式错了 / 字段类型不对）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(400, "请求体格式错误");
    }

    /**
     * 方法级参数校验失败（类上有 @Validated，参数上有 @Size / @NotBlank 时抛这个）。
     * 修：之前没单独处理它，被兜底的 Exception 吃掉，返 500"系统繁忙"，
     * 而实际只是"用户名长度不对"。日志里能看到 ConstraintViolationException。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400, message);
    }

    /**
     * 未登录 / token 失效 → 401。
     * 和业务错误（400）区分开：401 表示"你得先登录"，前端据此跳登录页。
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleUnauthorized(UnauthorizedException e) {
        return Result.error(401, e.getMessage());
    }

    /**
     * 请求的路径不存在 → 404。
     *
     * 修：之前被兜底的 Exception 吃掉，返 500"系统繁忙"。
     * 但"URL 写错了"根本不是服务器故障，应该是 404。
     * 这个很常见：前端接口名和后端对不上（比如前端调 /user/updateAvatar，
     * 后端实际是 /user/updateA），排查时如果返 500 会往错的方向查。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoResourceFound(NoResourceFoundException e) {
        return Result.error(404, "接口不存在：" + e.getResourcePath());
    }

    /**
     * 请求方法不支持（比如用 GET 调了一个只支持 POST 的接口）→ 405。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "请求方式不正确，请使用 " + e.getSupportedHttpMethods());
    }

    /**
     * 兜底：真正的服务端故障。
     * 堆栈只打进日志，返给前端的是模糊提示（不外泄内部细节）。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
