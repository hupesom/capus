package com.example.trade.mapper;

import com.example.trade.entity.FavoriteItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 12:52
 */
@Mapper
public interface FavoritesMapper {

    /**
     * 收藏商品 —— 回答"怎么知道这个商品上没上架、存不存在"。
     *
     * 用一条 INSERT ... SELECT 完成「存在性校验 + 在售校验 + 插入」三件事：
     *   insert into favorites(user_id, goods_id)
     *   select #{userId}, id from goods
     *   where id = #{goodsId} and status = 1
     *
     * 含义：只有"商品存在 且 状态是在售(1)"时，才会真的插入一行。
     * 商品不存在或已下架 → SELECT 查不到行 → INSERT 影响 0 行 → 返回 0。
     *
     * 为什么不用"先查再插"：
     *   先 select 判断商品在售、再 insert，两条语句之间有时间窗口，
     *   商品可能恰好在这个窗口里被别人买走（下架）。
     *   一条 SQL 由数据库保证原子性，没有竞态窗口。
     *
     * 返回值：1 = 收藏成功，0 = 商品不存在或已下架
     */
    @Insert("insert into favorites(user_id,goods_id) " +
            "select #{userId}, id from goods where id = #{goodsId} and status = 1")
    int add(@Param("goodsId") Long goodsId, @Param("userId") Integer userId);

    /** 取消收藏：只能取消自己的 */
    @Delete("delete from favorites where user_id=#{userId} and goods_id=#{goodsId}")
    int delete(@Param("goodsId") Long goodsId, @Param("userId") Integer userId);

    /** 我的收藏列表（连表查商品信息，否则前端只拿到两个 id 没法显示） */
    List<FavoriteItem> list(@Param("userId") Integer userId);
}
