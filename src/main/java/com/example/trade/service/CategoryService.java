package com.example.trade.service;

import com.example.trade.entity.Category;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 9:41
 */
public interface CategoryService {
    void add(String categoryName, String categoryAlias);

    /** 所有类目，人人可见 */
    List<Category> list();

    Category detail(Long id);

    void update(Category category);

    void delete(Long id);
}
