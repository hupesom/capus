package com.example.trade.service.impl;

import com.example.trade.entity.Category;
import com.example.trade.mapper.CategoryMapper;
import com.example.trade.service.CategoryService;
import com.example.trade.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 9:41
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void add(String categoryName, String categoryAlias) {
        Integer userId = ThreadLocalUtil.getUserId();
        categoryMapper.add(categoryName, categoryAlias, userId);
    }

    /**
     * 所有类目，人人可见。
     * 修：原来 where create_user=#{id} 只能看到自己建的类目，
     * 买家要按类目筛选商品，必须能看到全部类目。
     */
    @Override
    public List<Category> list() {
        return categoryMapper.list();
    }

    @Override
    public Category detail(Long id) {
        return categoryMapper.detail(id);
    }

    /** 只能改自己建的类目（SQL 里带 create_user 条件） */
    @Override
    public void update(Category category) {
        Integer userId = ThreadLocalUtil.getUserId();
        categoryMapper.update(category, userId);
    }

    /** 只能删自己建的类目 */
    @Override
    public void delete(Long id) {
        Integer userId = ThreadLocalUtil.getUserId();
        categoryMapper.delete(id, userId);
    }
}
