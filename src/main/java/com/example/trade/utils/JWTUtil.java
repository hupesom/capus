package com.example.trade.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类。
 *
 * 修：genToken 的参数上挂了 @Size(min=5,max=16) —— 那是对字符串长度的约束，
 * 贴在 Map 上毫无意义（而且这个方法根本不走校验流程），已删掉。
 *
 * @author maofan
 * @create 2026-10-24 23:29
 */
public class JWTUtil {

    /** 签名密钥。真实项目里应从配置读取，不要硬编码 */
    private static final String KEY = "study";

    /** token 有效期：1 小时 */
    private static final long EXPIRE_MILLIS = 1000L * 60 * 60;

    /**
     * 生成 token：把用户信息放进 claims，1 小时后过期。
     */
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_MILLIS))
                .sign(Algorithm.HMAC256(KEY));
    }

    /**
     * 解析并验签 token。
     * 密钥不对 / token 过期 / 被篡改，都会抛 JWTVerificationException，
     * 由调用方（拦截器）统一转成 401。
     */
    public static Map<String, Object> parseToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(KEY)).build().verify(token);
        Map<String, Object> claims = jwt.getClaim("claims").asMap();
        return claims == null ? new HashMap<>() : claims;
    }
}
