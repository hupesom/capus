package com.example.trade.service.impl;

import com.example.trade.entity.Goods;
import com.example.trade.entity.Orders;
import com.example.trade.entity.PageBean;
import com.example.trade.except.BusinessException;
import com.example.trade.mapper.GoodsMapper;
import com.example.trade.mapper.OrdersMapper;
import com.example.trade.service.OrdersService;
import com.example.trade.utils.ThreadLocalUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * @author maofan
 * @create 2026-10-26 14:08
 */
@Service
public class OrdersServiceImpl implements OrdersService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 下单。
     *
     * 这个方法做三件事，所以必须是 @Transactional：
     *   1. 插入订单记录
     *   2. 把商品状态改成"已售出"
     * 任一步失败两步一起回滚，否则会出现"商品被标记卖掉但订单不存在"。
     *
     * 防价格篡改：orders.setPrice(goods.getPrice()) —— 价格从商品表取，
     * 客户端传什么价格都不认。否则用户可以把价格改成 0.01 买走别人的东西。
     *
     * 防一物二卖：markSold 的 SQL 带 and status=1 乐观锁，
     * 返回 0 说明已经被别人抢先买走了 → 抛异常 → 事务回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Orders orders) {
        Integer buyerId = ThreadLocalUtil.getUserId();

        if (orders.getGoodsId() == null) {
            throw new BusinessException("请选择要购买的商品");
        }

        Goods goods = goodsMapper.detail(orders.getGoodsId());
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BusinessException("商品已售出或已下架");
        }
        if (goods.getSellerId() != null && goods.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能购买自己发布的商品");
        }

        // 关键：价格以服务端查到的为准，忽略客户端传的 price
        orders.setPrice(goods.getPrice());
        orders.setSellerId(goods.getSellerId());
        orders.setBuyerId(buyerId);
        orders.setOrderNo(generateOrderNo());

        ordersMapper.add(orders);

        // 乐观锁：status=1 才改得动。返回 0 = 已被别人买走
        int rows = goodsMapper.markSold(goods.getId());
        if (rows == 0) {
            throw new BusinessException("手慢了，该商品已被其他买家买走");
        }
    }

    /**
     * 订单号：时间戳 + 随机串。
     * 原来写死成 "123"，orders 表有唯一索引 uk_order_no，第二单直接撞唯一键报错。
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + random;
    }

    @Override
    public PageBean<Orders> mine(String role, Integer pageNum, Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();
        PageBean<Orders> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Orders> list = ordersMapper.mine(role, userId);

        PageInfo<Orders> pageInfo = new PageInfo<>(list);
        pageBean.setItems(pageInfo.getList());
        pageBean.setTotal((int) pageInfo.getTotal());
        return pageBean;
    }

    /**
     * 确认收货 / 取消订单。
     * 修：原来把取当前用户的代码注释掉了，谁都能改别人的订单。
     * 现在 SQL 里带 (buyer_id=当前用户 or seller_id=当前用户) 归属判断。
     */
    @Override
    public void confirm(Integer status, Long id) {
        if (status == null || (status != 2 && status != 3)) {
            throw new BusinessException("订单状态只能是 2(确认收货) 或 3(取消)");
        }
        Integer userId = ThreadLocalUtil.getUserId();
        int rows = ordersMapper.confirm(status, id, userId);
        if (rows == 0) {
            throw new BusinessException("订单不存在或无权操作");
        }
    }
}
