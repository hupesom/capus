package com.example.trade.service.impl;

import com.example.trade.entity.FavoriteItem;
import com.example.trade.entity.PageBean;
import com.example.trade.except.BusinessException;
import com.example.trade.mapper.FavoritesMapper;
import com.example.trade.service.FavoritesService;
import com.example.trade.utils.ThreadLocalUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 12:53
 */
@Service
public class FavoritesServiceImpl implements FavoritesService {

    @Autowired
    private FavoritesMapper favoritesMapper;

    /**
     * 收藏商品。
     *
     * 「商品存不存在、上没上架」这个问题，在 Mapper 里用一条
     *   INSERT ... SELECT ... WHERE status = 1
     * 解决：商品不存在或已下架 → 影响 0 行 → 这里抛业务异常。
     * 一条 SQL 完成校验+插入，没有"先查再插"的竞态窗口。
     */
    @Override
    public void add(Long goodsId) {
        Integer userId = ThreadLocalUtil.getUserId();
        try {
            int rows = favoritesMapper.add(goodsId, userId);
            if (rows == 0) {
                throw new BusinessException("商品不存在或已下架，无法收藏");
            }
        } catch (DuplicateKeyException e) {
            // favorites 表有唯一索引 uk_user_goods(user_id, goods_id)，重复收藏会撞唯一键
            throw new BusinessException("该商品已收藏，请勿重复操作");
        }
    }

    @Override
    public void delete(Long goodsId) {
        Integer userId = ThreadLocalUtil.getUserId();
        favoritesMapper.delete(goodsId, userId);
    }

    /**
     * 我的收藏列表（连表查出商品信息）。
     */
    @Override
    public PageBean<FavoriteItem> list(Integer pageNum, Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();
        PageBean<FavoriteItem> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<FavoriteItem> list = favoritesMapper.list(userId);

        PageInfo<FavoriteItem> pageInfo = new PageInfo<>(list);
        pageBean.setTotal((int) pageInfo.getTotal());
        pageBean.setItems(pageInfo.getList());
        return pageBean;
    }
}
