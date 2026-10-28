package com.example.trade.service;

import com.example.trade.entity.Orders;
import com.example.trade.entity.PageBean;

/**
 * @author maofan
 * @create 2026-10-26 14:08
 */
public interface OrdersService {

    /** 下单：价格由服务端从商品表取，客户端传的价格一律忽略 */
    void add(Orders orders);

    /**
     * 我的订单。
     * @param role 'buy' 我买的 / 'sell' 我卖的
     */
    PageBean<Orders> mine(String role, Integer pageNum, Integer pageSize);

    /**
     * 确认收货 / 取消订单。
     * @param status 2 = 已完成（确认收货），3 = 已取消
     */
    void confirm(Integer status, Long id);
}
