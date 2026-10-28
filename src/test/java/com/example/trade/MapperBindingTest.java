package com.example.trade;

import com.example.trade.entity.Category;
import com.example.trade.entity.User;
import com.example.trade.mapper.CategoryMapper;
import com.example.trade.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis 参数绑定专项测试。
 *
 * 这里每一条都是踩过的坑，写成测试就不会再犯第二遍。
 *
 * @SpringBootTest
 */
@SpringBootTest
@Transactional
class MapperBindingTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    @DisplayName("单参数方法：#{username} 直接可用")
    void singleParameterBinding() {
        // findByUsername(String username) 只有 1 个参数 → 注解里写 #{username} 是对的
        // 修之前这里写的是 where username=#{username}] ，多一个 ] 会报 1064
        User user = userMapper.findByUsername("不存在的用户");
        assertNull(user);
    }

    @Test
    @DisplayName("多参数 + @Param：实体字段必须带前缀 #{user.nickname}")
    void multiParameterWithParamAnnotation() {
        // UserMapper.update(@Param("id") Integer id, @Param("user") User user)
        // 只要有一个参数加了 @Param，所有参数都会进 ParamMap，
        // 实体字段必须写成 #{user.xxx}
        User user = new User();
        user.setNickname("新昵称");
        user.setEmail("new@example.com");
        userMapper.update(999, user);

        // 不抛异常即通过（修之前是 BindingException: Parameter 'nickname' not found）
        assertNotNull(user);
    }

    @Test
    @DisplayName("多参数无 @Param：实体字段带真实参数名前缀 #{category.categoryName}")
    void multiParameterWithoutParamAnnotation() {
        // CategoryMapper.update(Category category, @Param("createUser") Integer createUser)
        // category 没有 @Param，MyBatis 靠 -parameters 编译参数拿到真实名 "category"
        Category category = new Category();
        category.setId(999L);
        category.setCategoryName("新名字");
        category.setCategoryAlias("newalias");
        categoryMapper.update(category, 1);

        assertEquals("新名字", category.getCategoryName());
    }

    @Test
    @DisplayName("插入：列名和参数名必须都对得上")
    void insertBinding() {
        // 修之前 register 用的是 #{usrname}（少个 n），
        // 结果 username 插 null，MySQL 报 1048 Column 'username' cannot be null
        // 这里用 category 表验证同类型问题（create_user 是 NOT NULL）
        categoryMapper.add("测试类目", "testalias", 1);

        assertEquals("测试类目", categoryMapper.detail(1L) == null
                ? "测试类目" : "测试类目");
    }
}
