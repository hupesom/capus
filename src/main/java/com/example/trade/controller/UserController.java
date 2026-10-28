package com.example.trade.controller;

import com.example.trade.entity.Result;
import com.example.trade.entity.User;
import com.example.trade.entity.UserUpdateDTO;
import com.example.trade.except.BusinessException;
import com.example.trade.service.UserService;
import com.example.trade.utils.JWTUtil;
import com.example.trade.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author maofan
 * @create 2026-10-24 21:16
 */
@Validated
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserService userService;

    /**
     * 注册。
     * @NotBlank + @Size 都要有：@Size 对 null 是放行的，只有 @NotBlank 才能挡住空值。
     *
     * ⚠️ 错误处理统一用 throw new BusinessException(...)，
     *    不用 return Result.error(...)。
     *    原因：Result.error 走的是 HTTP 200，BusinessException 走 HTTP 400，
     *    两种混着用前端没法统一判断。全部交给 GlobalExceptionHandler 处理。
     */
    @PostMapping("register")
    public Result register(@NotBlank(message = "用户名不能为空") @Size(min = 5, max = 16, message = "用户名长度需在 5~16 之间") String username,
                           @NotBlank(message = "密码不能为空") @Size(min = 5, max = 16, message = "密码长度需在 5~16 之间") String password) {
        User byUsername = userService.findByUsername(username);
        if (byUsername != null) {
            throw new BusinessException("用户已存在");
        }
        userService.register(username, password);
        return Result.success();
    }

    /**
     * 登录：成功后签发 token 并存进 Redis（1 小时有效）。
     * Redis 里的 token 是"黑名单"机制：改密码时删掉它，用户就立刻掉线。
     */
    @PostMapping("login")
    public Result login(@NotBlank(message = "用户名不能为空") @Size(min = 5, max = 16, message = "用户名长度需在 5~16 之间") String username,
                        @NotBlank(message = "密码不能为空") @Size(min = 5, max = 16, message = "密码长度需在 5~16 之间") String password) {
        User byUsername = userService.findByUsername(username);
        if (byUsername == null) {
            // 不提示"用户不存在"还是"密码错误" —— 一样都是"用户名或密码错误"，
            // 避免有人拿接口枚举出哪些用户名注册过
            throw new BusinessException("用户名或密码错误");
        }
        String s = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!byUsername.getPassword().equals(s)) {
            throw new BusinessException("用户名或密码错误");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", byUsername.getId());
        map.put("username", byUsername.getUsername());
        String token = JWTUtil.genToken(map);
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set(token, token, 1, TimeUnit.HOURS);
        return Result.success(token);
    }

    @GetMapping("userInfo")
    public Result<User> userInfo() {
        User user = userService.userInfo();
        return Result.success(user);
    }

    /**
     * 修改资料。
     * 加了 @Valid，User 实体上的 @NotBlank / @Size / @Email 才会真正执行。
     */
    @PutMapping("update")
    public Result update(@Valid @RequestBody User user) {
        userService.update(user);
        return Result.success();
    }

    /**
     * 修改头像。
     *
     * 修：原来路径叫 "updateA"（updateAvatar 的缩写），看不出是干什么的，
     * 而且我实测时用 /user/updateAvatar 调用会 404。
     * 现在改成 updateAvatar，旧的 updateA 路径保留（不破坏已有调用）。
     *
     * @PatchMapping 可以写多个路径：@PatchMapping({"updateAvatar", "updateA"})
     */
    @PatchMapping({"updateAvatar", "updateA"})
    public Result updateA(@Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        userService.updateA(userUpdateDTO.getAvatarUrl());
        return Result.success();
    }

    /**
     * 修改密码：改成功后删掉 Redis 里的 token，强制重新登录。
     */
    @PatchMapping("updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> map, @RequestHeader("Authorization") String token) {
        String oldPwd = map.get("old_pwd");
        String newPwd = map.get("new_pwd");
        String rePwd = map.get("re_pwd");
        if (oldPwd == null || newPwd == null || rePwd == null) {
            throw new BusinessException("输入参数不完整");
        }
        if (!newPwd.equals(rePwd)) {
            throw new BusinessException("两次密码输入不一致");
        }
        String username = (String) ThreadLocalUtil.get().get("username");
        User byUsername = userService.findByUsername(username);
        if (byUsername == null) {
            throw new BusinessException("用户不存在");
        }
        String s = DigestUtils.md5DigestAsHex(oldPwd.getBytes());
        if (!s.equals(byUsername.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        String lastPwd = DigestUtils.md5DigestAsHex(rePwd.getBytes());
        userService.updatePwd(username, lastPwd);
        // 强制下线：删掉 token，之后用旧 token 请求会 401
        stringRedisTemplate.delete(token);
        return Result.success();
    }
}
