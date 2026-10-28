package com.example.trade.service;


import com.example.trade.entity.User;

/**
 * @author maofan
 * @create 2026-10-24 21:18
 */
public interface UserService {
    User findByUsername(String username);

    void register(String username, String password);

    User userInfo();

    void update(User user);

    void updateA(String avatarUrl);

    void updatePwd(String username, String lastPwd);
}
