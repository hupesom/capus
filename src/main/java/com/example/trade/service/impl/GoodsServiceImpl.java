package com.example.trade.service.impl;

import com.example.trade.entity.Goods;
import com.example.trade.entity.PageBean;
import com.example.trade.except.BusinessException;
import com.example.trade.mapper.GoodsMapper;
import com.example.trade.service.GoodsService;
import com.example.trade.utils.ThreadLocalUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 10:18
 */
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 发布商品。
     * seller_id 和 create_user 都从 ThreadLocal 取 —— 卖家是谁由服务端决定，
     * 客户端传什么不算数（否则就能冒充别人卖东西）。
     */
    @Override
    public void add(Goods goods) {
        Integer userId = ThreadLocalUtil.getUserId();
        goods.setCreateUser(userId);
        goods.setSellerId(userId);
        goodsMapper.add(goods);
    }

    /**
     * 商品列表（买家视角）：不按 create_user 过滤，买家能看到所有人的商品。
     */
    @Override
    public PageBean<Goods> get(Integer pageNum, Integer pageSize, Integer categoryId, Integer status, String keyword) {
        PageBean<Goods> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Goods> goodsList = goodsMapper.get(categoryId, status, keyword);

        PageInfo<Goods> pageInfo = new PageInfo<>(goodsList);
        pageBean.setTotal((int) pageInfo.getTotal());
        pageBean.setItems(pageInfo.getList());
        return pageBean;
    }

    /**
     * 商品详情 + 浏览量自增。
     * 浏览量单独一个方法，是因为原来把两条 SQL 塞进一个 @Select 里，
     * 那叫多语句，JDBC 默认不允许（要额外开 allowMultiQueries）。
     */
    @Override
    public Goods detail(Long id) {
        Goods goods = goodsMapper.detail(id);
        if (goods == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        goodsMapper.incrViewCount(id);
        goods.setViewCount(goods.getViewCount() + 1);
        return goods;
    }

    /**
     * 编辑商品：SQL 里带了 create_user 条件，只能改自己发布的。
     */
    @Override
    public void update(Goods goods) {
        Integer userId = ThreadLocalUtil.getUserId();
        goodsMapper.update(goods, userId);
    }

    /**
     * 上架 / 下架。
     * status 只允许 1（在售）或 3（下架）—— 2（已售出）是系统状态，不该手工设。
     */
    @Override
    public void status(Long id, Integer status) {
        if (status == null || (status != 1 && status != 3)) {
            throw new BusinessException("商品状态只能是 1(上架) 或 3(下架)");
        }
        Integer userId = ThreadLocalUtil.getUserId();
        goodsMapper.status(userId, id, status);
    }

    @Override
    public void delete(Long id) {
        Integer userId = ThreadLocalUtil.getUserId();
        goodsMapper.delete(id, userId);
    }

    /**
     * 我发布的商品（卖家视角，含已售出/已下架）。
     */
    @Override
    public PageBean<Goods> mine(Integer pageNum, Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();
        PageBean<Goods> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Goods> list = goodsMapper.mine(userId);

        PageInfo<Goods> pageInfo = new PageInfo<>(list);
        pageBean.setTotal((int) pageInfo.getTotal());
        pageBean.setItems(pageInfo.getList());
        return pageBean;
    }
}
