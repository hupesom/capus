package com.example.trade.except;

/**
 * 未认证异常：没登录、token 失效、被强制下线。
 *
 * 和 BusinessException 分开是因为语义不同：
 *   BusinessException → 400，用户的操作不合理，但身份是有效的
 *   UnauthorizedException → 401，身份本身有问题，前端该跳登录页
 *
 * @author maofan
 * @create 2026-10-27 10:50
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
