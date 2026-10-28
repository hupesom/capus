package com.example.trade.mapper;

import com.example.trade.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * @author maofan
 * @create 2026-10-24 21:18
 */
@Mapper
public interface UserMapper {

    /** 修：原来结尾多了一个 ]，MySQL 报 1064 语法错误 */
    @Select("select * from user where username=#{username}")
    User findByUsername(String username);

    /** 修：原来写的是 #{usrname}（少个 n），实际插入 username 为 null，MySQL 报 1048 */
    @Insert("insert into user (username,password,create_time,update_time) values (#{username},#{password},now(),now())")
    void register(@Param("username") String username, @Param("password") String password);

    @Select("select * from user where id = #{id}")
    User userInfo(Integer id);

    /**
     * 修改资料。SQL 写在 UserMapper.xml。
     *
     * 修：User user 是多参数里的普通对象，注解里必须写 #{user.nickname}，
     * 而且不能更新 username / password（改密码有专门的接口）。
     */
    void update(@Param("id") Integer id, @Param("user") User user);

    /** 修：列名是 user_pic，不是 UserPic（MySQL 报 1054 未知列） */
    @Update("update user set user_pic=#{avatarUrl} where id = #{id}")
    void updateA(@Param("id") Integer id, @Param("avatarUrl") String avatarUrl);

    /** 修：原来用 #{newPwd}，但方法参数名是 lastPwd，绑定不上 */
    @Update("update user set password=#{lastPwd} where username=#{username}")
    void updatePwd(@Param("username") String username, @Param("lastPwd") String lastPwd);
}
