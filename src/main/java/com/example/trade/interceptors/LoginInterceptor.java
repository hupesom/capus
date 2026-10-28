package com.example.trade.interceptors;

import com.example.trade.entity.Result;
import com.example.trade.utils.JWTUtil;
import com.example.trade.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * 登录校验拦截器。
 *
 * 流程：
 *   1. 从请求头拿 Authorization
 *   2. 兼容 "Bearer xxx" 格式，去掉前缀
 *   3. 去 Redis 查这个 token 是否还有效（改密码 / 退出登录会删掉它 → 强制下线）
 *   4. 解析 token 拿到用户信息，塞进 ThreadLocal
 *   5. 放行；任何一步失败都返 401
 *
 * 修：原来 import 了 jogamp.common.util.locks.RecursiveThreadGroupLockImpl01Unfairish
 *     —— IDEA 自动导入的垃圾包，编译直接失败："程序包jogamp.common.util.locks不存在"。
 *
 * @author maofan
 * @create 2026-10-24 23:26
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 免登录的接口（只读，游客也能看）。
     *
     * ⚠️ 为什么要按"方法 + 路径"两个条件判断，不能只看路径？
     *   因为同一个路径上不同方法的语义完全不同：
     *     GET  /category  查类目   → 游客逛商品要看类目，免登录
     *     POST /category  新增类目 → 要记录 create_user（谁建的），必须登录
     *   Spring 的 excludePathPatterns 只按路径匹配，不区分方法，
     *   所以免登录规则只能放在拦截器里判断。
     *
     * 写操作（POST/PUT/DELETE/PATCH）一律要登录。
     */
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/goods",          // 商品列表
            "/goods/detail",   // 商品详情
            "/category"        // 类目列表
    );

    private static final Set<String> PUBLIC_ALL_PATHS = Set.of(
            "/user/login",
            "/user/register"
    );

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 关键：请求的路径根本不存在时（handler 不是 HandlerMethod），
        // 直接放行，让请求继续走到 DispatcherServlet，由它抛 404。
        // 不放行的话，未登录用户访问一个不存在的接口会收到 401 而不是 404，
        // 排查时会被误导成"登录有问题"，实际是"接口名写错了"。
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 免登录接口直接放行
        if (isPublic(request)) {
            return true;
        }

        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        // 兼容前端带 "Bearer " 前缀的情况
        if (token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }

        try {
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            // Redis 里没有 = 已退出登录 / 已改密码被强制下线
            String redisToken = operations.get(token);
            if (redisToken == null) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return false;
            }
            Map<String, Object> map = JWTUtil.parseToken(redisToken);
            ThreadLocalUtil.set(map);
            return true;
        } catch (Exception e) {
            // token 过期 / 被篡改 / Redis 挂了，一律 401
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }
    }

    /**
     * 判断这个请求是否免登录。
     * 规则：免登录名单里的路径 + 只允许 GET（写操作永远要登录）。
     */
    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PUBLIC_ALL_PATHS.contains(path)) {
            return true;
        }
        // 只读接口才免登录，写操作（增删改）一律要登录
        boolean isReadMethod = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        return isReadMethod && PUBLIC_GET_PATHS.contains(path);
    }

    /**
     * 返回 401 + 统一 JSON 结构。
     *
     * 为什么要自己写 JSON：
     *   拦截器抛的异常不会被 @RestControllerAdvice 捕获（那只管 Controller 里的异常），
     *   直接 response.setStatus(401) 的话前端收到的是空 body，
     *   拿不到"请先登录"这个提示。这里手动写回去。
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        response.getWriter().write(json);
    }

    /**
     * 请求结束后清理 ThreadLocal。
     * 必须做：Tomcat 线程池会复用线程，不清理会导致下一个请求读到上一个人的身份。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
