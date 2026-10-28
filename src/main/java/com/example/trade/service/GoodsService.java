package com.example.trade.service;

import com.example.trade.entity.Goods;
import com.example.trade.entity.PageBean;

/**
 * @author maofan
 * @create 2026-10-26 10:18
 */
public interface GoodsService {

    void add(Goods goods);

    /**
     * 商品列表（买家视角）。
     * @param categoryId 类目筛选，可为 null
     * @param status     状态筛选，可为 null（不筛选）
     * @param keyword    关键词模糊搜索，可为 null
     */
    PageBean<Goods> get(Integer pageNum, Integer pageSize, Integer categoryId, Integer status, String keyword);

    Goods detail(Long id);

    void update(Goods goods);

    /** 上架(1) / 下架(3) */
    void status(Long id, Integer status);

    void delete(Long id);

    PageBean<Goods> mine(Integer pageNum, Integer pageSize);
}
