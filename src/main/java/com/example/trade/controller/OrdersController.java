package com.example.trade.controller;

import com.example.trade.entity.Orders;
import com.example.trade.entity.PageBean;
import com.example.trade.entity.Result;
import com.example.trade.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author maofan
 * @create 2026-10-26 14:07
 */
@RestController
@RequestMapping("orders")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    /**
     * 下单。
     * 客户端只需要传 goodsId，价格和卖家由服务端确定。
     */
    @PostMapping
    public Result add(@RequestBody Orders orders) {
        ordersService.add(orders);
        return Result.success();
    }

    /**
     * 我的订单：role = 'buy' 我买的 / 'sell' 我卖的。
     */
    @GetMapping("mine")
    public Result<PageBean<Orders>> mine(String role, Integer pageNum, Integer pageSize) {
        PageBean<Orders> pageBean = ordersService.mine(role, pageNum, pageSize);
        return Result.success(pageBean);
    }

    /**
     * 确认收货(2) / 取消订单(3)。
     */
    @PatchMapping("confirm")
    public Result confirm(Integer status, Long id) {
        ordersService.confirm(status, id);
        return Result.success();
    }
}
