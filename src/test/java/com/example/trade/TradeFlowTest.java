package com.example.trade;

import com.example.trade.entity.Goods;
import com.example.trade.entity.Orders;
import com.example.trade.except.BusinessException;
import com.example.trade.mapper.GoodsMapper;
import com.example.trade.mapper.OrdersMapper;
import com.example.trade.service.FavoritesService;
import com.example.trade.service.GoodsService;
import com.example.trade.service.OrdersService;
import com.example.trade.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务逻辑集成测试（跑在真实 MySQL 上）。
 *
 * 为什么用测试而不是 curl：
 *   - 不需要在 HTTP 层带 token，测试里直接构造 ThreadLocal 身份
 *   - 断言写死在代码里，每次改完代码跑一遍就知道有没有回归
 *   - 这是你简历项目里能加分的东西：真实测试类，不是手动点接口
 *
 * ⚠️ 前置条件：application.yml 里的 Redis 必须能连上（本机 127.0.0.1:6379），
 *    否则 Spring 上下文起不来。
 */
@SpringBootTest
@Transactional
class TradeFlowTest {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private FavoritesService favoritesService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    /** 模拟当前登录用户 */
    private void loginAs(int userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", userId);
        map.put("username", "tester" + userId);
        ThreadLocalUtil.set(map);
    }

    @BeforeEach
    void setUp() {
        // 造一个卖家(1) 和一个买家(2)
        loginAs(1);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    @DisplayName("商品列表是买家视角：能看到别人发布的商品")
    void buyerCanSeeOthersGoods() {
        // 卖家 1 发布商品
        Goods goods = new Goods();
        goods.setTitle("测试商品-可见性");
        goods.setDescription("买家应该能看到");
        goods.setPrice(new java.math.BigDecimal("100.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);

        // 买家 2 去查列表，应该看得到
        loginAs(2);
        var page = goodsService.get(1, 10, null, null, null);
        assertTrue(page.getTotal() > 0, "买家应该能看到别人发布的商品");
    }

    @Test
    @DisplayName("关键词搜索：按标题模糊匹配")
    void keywordSearchWorks() {
        Goods goods = new Goods();
        goods.setTitle("二手自行车九成新");
        goods.setDescription("代步用");
        goods.setPrice(new java.math.BigDecimal("150.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);

        var hit = goodsService.get(1, 10, null, null, "自行车");
        assertTrue(hit.getTotal() > 0, "关键词应该能搜到");

        var miss = goodsService.get(1, 10, null, null, "不存在的词xyz");
        assertEquals(0, miss.getTotal(), "无关关键词应该搜不到");
    }

    @Test
    @DisplayName("编辑商品：不能改别人发布的")
    void cannotEditOthersGoods() {
        Goods goods = new Goods();
        goods.setTitle("我的商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("50.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        // 换成买家 2 去改 1 的商品
        loginAs(2);
        Goods update = new Goods();
        update.setId(goodsId);
        update.setTitle("被篡改");
        update.setDescription("hack");
        update.setPrice(new java.math.BigDecimal("1.00"));
        update.setCategoryId(1L);
        goodsService.update(update);

        // 查库确认标题没变
        Goods after = goodsMapper.detail(goodsId);
        assertEquals("我的商品", after.getTitle(), "不应该能改别人的商品");
    }

    @Test
    @DisplayName("收藏：商品不存在时抛业务异常")
    void favoriteNonExistingGoodsThrows() {
        loginAs(2);
        BusinessException e = assertThrows(BusinessException.class,
                () -> favoritesService.add(999999L));
        assertTrue(e.getMessage().contains("不存在") || e.getMessage().contains("下架"));
    }

    @Test
    @DisplayName("收藏：商品存在且在售才能收藏成功")
    void favoriteExistingGoodsSucceeds() {
        Goods goods = new Goods();
        goods.setTitle("可收藏的商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("80.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        loginAs(2);
        favoritesService.add(goodsId);   // 不抛异常即通过
    }

    @Test
    @DisplayName("下单：价格以服务端为准，客户端传的 0.01 被忽略")
    void orderPriceComesFromServer() {
        Goods goods = new Goods();
        goods.setTitle("防篡改商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("150.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        loginAs(2);
        Orders orders = new Orders();
        orders.setGoodsId(goodsId);
        orders.setPrice(new java.math.BigDecimal("0.01"));   // 恶意价格
        ordersService.add(orders);

        // 查库：订单价格必须是 150
        var list = ordersMapper.mine("buy", 2);
        assertFalse(list.isEmpty(), "应该生成订单");
        assertEquals(0, new java.math.BigDecimal("150.00").compareTo(list.get(0).getPrice()),
                "订单价格必须来自商品表，不能是客户端传的 0.01");
    }

    @Test
    @DisplayName("下单：不能买自己发布的商品")
    void cannotBuyOwnGoods() {
        Goods goods = new Goods();
        goods.setTitle("自己的商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("60.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        Orders orders = new Orders();
        orders.setGoodsId(goodsId);
        BusinessException e = assertThrows(BusinessException.class, () -> ordersService.add(orders));
        assertTrue(e.getMessage().contains("自己"));
    }

    @Test
    @DisplayName("下单：商品被买走后状态变 2，且不能重复下单")
    void goodsBecomesSoldAndCannotBeBoughtTwice() {
        Goods goods = new Goods();
        goods.setTitle("唯一商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("300.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        loginAs(2);
        Orders o1 = new Orders();
        o1.setGoodsId(goodsId);
        ordersService.add(o1);

        // 状态应变成 2 已售出
        assertEquals(2, goodsMapper.detail(goodsId).getStatus(), "商品状态应变为已售出");

        // 再买一次应失败
        loginAs(3);
        Orders o2 = new Orders();
        o2.setGoodsId(goodsId);
        assertThrows(BusinessException.class, () -> ordersService.add(o2), "已售出的商品不能再次下单");
    }

    @Test
    @DisplayName("商品详情：浏览量自增")
    void detailIncrementsViewCount() {
        Goods goods = new Goods();
        goods.setTitle("有浏览量的商品");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("20.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        int before = goodsMapper.detail(goodsId).getViewCount();
        goodsService.detail(goodsId);
        goodsService.detail(goodsId);
        int after = goodsMapper.detail(goodsId).getViewCount();
        assertEquals(before + 2, after, "每次详情浏览量应 +1");
    }

    @Test
    @DisplayName("上架下架：只能操作自己发布的商品")
    void statusOnlyForOwner() {
        Goods goods = new Goods();
        goods.setTitle("自己的商品2");
        goods.setDescription("desc");
        goods.setPrice(new java.math.BigDecimal("30.00"));
        goods.setCategoryId(1L);
        goodsService.add(goods);
        Long goodsId = goodsMapper.mine(1).get(0).getId();

        // 买家 2 尝试下架
        loginAs(2);
        goodsService.status(goodsId, 3);
        assertEquals(1, goodsMapper.detail(goodsId).getStatus(), "别人不能改我的商品状态");

        // 卖家自己下架
        loginAs(1);
        goodsService.status(goodsId, 3);
        assertEquals(3, goodsMapper.detail(goodsId).getStatus(), "卖家应该能下架");
    }
}
