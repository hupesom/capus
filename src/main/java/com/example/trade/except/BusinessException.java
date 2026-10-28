package com.example.trade.except;

/**
 * 业务异常：由业务规则主动抛出，会被 GlobalException 捕获并原样返回 message。
 * 用来区分"用户操作不对"(4xx 语义) 和 "服务端炸了"(500)。
 *
 * @author maofan
 * @create 2026-10-27 10:20
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
