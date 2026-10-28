package com.example.trade.controller;

import com.example.trade.entity.FavoriteItem;
import com.example.trade.entity.PageBean;
import com.example.trade.entity.Result;
import com.example.trade.service.FavoritesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author maofan
 * @create 2026-10-26 12:52
 */
@Validated
@RestController
@RequestMapping("favorite")
public class FavoritesController {

    @Autowired
    private FavoritesService favoritesService;

    /**
     * 收藏商品。
     * 商品不存在或已下架会返回错误提示（BusinessException → 400）。
     */
    @PostMapping
    public Result add(Long goodsId) {
        favoritesService.add(goodsId);
        return Result.success();
    }

    /** 取消收藏 */
    @DeleteMapping
    public Result delete(Long goodsId) {
        favoritesService.delete(goodsId);
        return Result.success();
    }

    /** 我的收藏列表（带商品标题、价格、图片） */
    @GetMapping("mine")
    public Result<PageBean<FavoriteItem>> list(Integer pageNum, Integer pageSize) {
        PageBean<FavoriteItem> pageBean = favoritesService.list(pageNum, pageSize);
        return Result.success(pageBean);
    }
}
