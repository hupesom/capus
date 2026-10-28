package com.example.trade.utils;

import java.util.Map;

/**
 * 线程上下文：存放"当前登录用户"的信息。
 *
 * 为什么用 ThreadLocal：
 *   Tomcat 用线程池复用线程，一个线程处理完请求 A 后，
 *   如果不清理，下一个请求 B 复用同一线程时会读到 A 的用户信息。
 *   所以必须在 afterCompletion 里 remove()（见 LoginInterceptor）。
 *
 * @author maofan
 * @create 2026-10-25 21:17
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    /** 取当前登录用户信息；没登录时返回 null */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> get() {
        return (Map<String, Object>) THREAD_LOCAL.get();
    }

    /**
     * 取当前登录用户 id。
     *
     * 原来的写法是 (Integer) ThreadLocalUtil.get().get("id")，散落在每个 Service 里，
     * 每次都要强转、每次都可能 NPE。抽成方法后统一处理。
     *
     * ⚠️ 正常流程下，拦截器已经拦住未登录的请求了（返 401），
     *    所以走到 Service 时一定有登录态。
     *    这里的 null 分支是"兜底"：万一某个接口没被正确拦截，
     *    至少给一句明确提示，而不是 NullPointerException。
     */
    public static Integer getUserId() {
        Map<String, Object> map = get();
        if (map == null) {
            throw new com.example.trade.except.UnauthorizedException("请先登录");
        }
        Object id = map.get("id");
        if (id == null) {
            throw new com.example.trade.except.UnauthorizedException("登录信息已失效，请重新登录");
        }
        return (Integer) id;
    }

    public static void set(Map<String, Object> value) {
        THREAD_LOCAL.set(value);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
