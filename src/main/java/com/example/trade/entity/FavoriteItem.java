package com.example.trade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏列表项：收藏记录 + 商品信息（连表查出来的）。
 *
 * 为什么要单独建一个类：
 *   favorites 表里只有 user_id 和 goods_id，前端拿到后没法显示任何东西。
 *   联表查出来后列名会重复，所以用别名 + 这个 VO 承接。
 *
 * @author maofan
 * @create 2026-10-27 10:30
 */
@Data
public class FavoriteItem {

    /** 收藏记录 id */
    private Long id;

    /** 商品 id */
    private Long goodsId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 商品主图 */
    private String coverImg;

    /** 商品状态：1在售 2已售出 3已下架 4待审核 */
    private Integer goodsStatus;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
