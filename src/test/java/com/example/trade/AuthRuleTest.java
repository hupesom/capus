package com.example.trade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录放行规则测试。
 *
 * 这类测试的价值：以后你新增接口时，如果忘了加进拦截器的登录范围，
 * 或者错误地把写操作放行了，测试会立刻失败。
 *
 * ⚠️ Boot 4 的 MockMvc 用法有两个变化（和 Boot 3 不同）：
 *   1. @AutoConfigureMockMvc 的包名变成 org.springframework.boot.webmvc.test.autoconfigure
 *   2. get()/post() 的参数变成了可变参数 Object...，
 *      所以不能再写 get("/goods", 1, 10) 传两个数字 ——
 *      那两个数字会被当成 URI 模板变量。要用 queryParam("pageNum", "1")。
 *
 * ⚠️ 断言方式：业务失败时 HTTP 状态码是 400，错误信息在 body 的 code 字段里。
 *    code 和 HTTP 状态码保持一致（0成功 / 400业务错误 / 401未登录 / 404不存在 / 500故障）。
 *    所以判断"是否被拦截"要看 code==401，不能只看 HTTP 状态。
 *
 * @SpringBootTest
 * @AutoConfigureMockMvc
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("免登录：注册、登录（能到业务逻辑，不是 401）")
    void registerAndLoginArePublic() throws Exception {
        // 400 = 业务错误"用户已存在"，说明请求打到了 Controller，没被拦成 401
        mockMvc.perform(post("/user/register")
                        .param("username", "testuser1")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户已存在"));

        // 同理：用户名不存在 → 400
        mockMvc.perform(post("/user/login")
                        .param("username", "nobody_here")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("免登录：查商品列表、商品详情、类目列表")
    void readApisArePublic() throws Exception {
        mockMvc.perform(get("/goods")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 商品 id=1 不存在 → 业务错误 400，但仍不是 401（说明没被登录拦截）
        mockMvc.perform(get("/goods/detail").param("id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("要登录：写操作（新增类目、发布商品、收藏、下单）")
    void writeApisRequireLogin() throws Exception {
        // 关键：这些是写操作，必须 401，不能被误放行
        mockMvc.perform(post("/category")
                        .param("categoryName", "x")
                        .param("categoryAlias", "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/goods").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/favorite").param("goodsId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/orders").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("要登录：我的收藏、我的订单、我的发布")
    void myApisRequireLogin() throws Exception {
        mockMvc.perform(get("/favorite/mine")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/orders/mine")
                        .param("role", "buy")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/goods/mine")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("接口不存在 → 404（不是 401）")
    void unknownPathReturns404() throws Exception {
        // 关键：不存在的接口应返 404。
        // 如果返 401，说明拦截器没放行，排查时会被误导成"登录有问题"
        mockMvc.perform(get("/user/definitelyNotExist"))
                .andExpect(status().isNotFound());
    }
}
