package com.example.trade.conf;

import com.example.trade.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册。
 *
 * ⚠️ 这里**故意不写 excludePathPatterns**。
 *
 * 原因：Spring 的 excludePathPatterns 只按**路径**匹配，不区分 HTTP 方法。
 * 而我们的免登录规则必须区分方法：
 *   GET  /category  查类目   → 免登录（游客逛商品要看类目）
 *   POST /category  新增类目 → 要登录（要记录 create_user 是谁建的）
 *
 * 同一个路径 "/category"，如果写进 excludePathPatterns，
 * 那"新增类目"也会被一起放行 —— 越权了。
 *
 * 所以免登录规则统一放在 LoginInterceptor.isPublic() 里判断，
 * 那里能同时拿到 HTTP 方法和路径。
 *
 * 放行规则一览（这是"你自己的决定"，面试会问）：
 *   免登录：POST /user/register、POST /user/login、
 *           GET /goods、GET /goods/detail、GET /category
 *   要登录：其余全部（发布商品、编辑、删除、收藏、订单、我的发布…）
 *
 * @author maofan
 * @create 2026-10-24 23:24
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截全部请求，具体的免登录判断交给 LoginInterceptor 内部按"方法+路径"决定
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**");
    }
}
