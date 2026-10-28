package com.example.trade.service;

import com.example.trade.entity.FavoriteItem;
import com.example.trade.entity.PageBean;

/**
 * @author maofan
 * @create 2026-10-26 12:53
 */
public interface FavoritesService {

    /** 收藏商品：商品不存在或已下架会抛 BusinessException */
    void add(Long goodsId);

    /** 取消收藏 */
    void delete(Long goodsId);

    /** 我的收藏列表（带商品信息） */
    PageBean<FavoriteItem> list(Integer pageNum, Integer pageSize);
}
