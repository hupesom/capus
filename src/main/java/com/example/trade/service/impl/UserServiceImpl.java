package com.example.trade.service.impl;

import com.example.trade.entity.User;
import com.example.trade.mapper.UserMapper;
import com.example.trade.service.UserService;
import com.example.trade.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Map;

/**
 * @author maofan
 * @create 2026-10-24 21:18
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public void register(String username, String password) {
        String s = DigestUtils.md5DigestAsHex(password.getBytes());
        userMapper.register(username,s);
    }

    @Override
    public User userInfo() {
        Map<String,Object> o = ThreadLocalUtil.get();
        Integer id = (Integer) o.get("id");
        return userMapper.userInfo(id);
    }

    @Override
    public void update(User user) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.update(id,user);
    }

    @Override
    public void updateA(String avatarUrl) {
        Map<String,Object> map  = ThreadLocalUtil.get();
       Integer id= (Integer) map.get("id");
       userMapper.updateA(id,avatarUrl);
    }

    @Override
    public void updatePwd(String username, String lastPwd) {
        userMapper.updatePwd(username,lastPwd);
    }
}
