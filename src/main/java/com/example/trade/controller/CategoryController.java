package com.example.trade.controller;

import com.example.trade.entity.Category;
import com.example.trade.entity.Result;
import com.example.trade.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 9:40
 */
@Validated
@RestController
@RequestMapping("category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result add(String categoryName, String categoryAlias) {
        categoryService.add(categoryName, categoryAlias);
        return Result.success();
    }

    /** 所有类目，人人可见（买家要按类目筛选商品） */
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> categories = categoryService.list();
        return Result.success(categories);
    }

    @GetMapping("detail")
    public Result<Category> detail(Long id) {
        Category category = categoryService.detail(id);
        return Result.success(category);
    }

    /** 只能改自己建的类目 */
    @PutMapping
    public Result update(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success();
    }

    /** 只能删自己建的类目 */
    @DeleteMapping
    public Result delete(Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
