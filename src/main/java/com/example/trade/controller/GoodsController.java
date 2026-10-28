package com.example.trade.controller;

import com.example.trade.entity.Goods;
import com.example.trade.entity.PageBean;
import com.example.trade.entity.Result;
import com.example.trade.service.GoodsService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author maofan
 * @create 2026-10-26 10:14
 */
@Validated
@RestController
@RequestMapping("goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 发布商品。
     * 参数级校验：开关是类上的 @Validated，规则是参数上的注解。
     * 少了规则这一步就等于没校验（这是最容易漏的一环）。
     */
    @PostMapping
    public Result add(@RequestBody Goods goods) {
        goodsService.add(goods);
        return Result.success();
    }

    /**
     * 商品列表（买家视角）。
     * 支持按类目筛选、按状态筛选、按关键词模糊搜索。
     */
    @GetMapping
    public Result<PageBean<Goods>> get(Integer pageNum,
                                       Integer pageSize,
                                       @RequestParam(required = false) Integer categoryId,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        PageBean<Goods> pageBean = goodsService.get(pageNum, pageSize, categoryId, status, keyword);
        return Result.success(pageBean);
    }

    @GetMapping("detail")
    public Result<Goods> detail(Long id) {
        Goods goods = goodsService.detail(id);
        return Result.success(goods);
    }

    /**
     * 编辑商品：只能改自己发布的（Service 传当前用户 id，SQL 里做归属判断）。
     */
    @PutMapping
    public Result update(@RequestBody Goods goods) {
        goodsService.update(goods);
        return Result.success();
    }

    /**
     * 上架(1) / 下架(3)。
     */
    @PatchMapping("status")
    public Result status(Long id, @NotNull Integer status) {
        goodsService.status(id, status);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(Long id) {
        goodsService.delete(id);
        return Result.success();
    }

    /**
     * 我发布的商品。
     */
    @GetMapping("mine")
    public Result<PageBean<Goods>> mine(Integer pageNum, Integer pageSize) {
        PageBean<Goods> pageBean = goodsService.mine(pageNum, pageSize);
        return Result.success(pageBean);
    }
}
