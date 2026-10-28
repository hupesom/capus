package com.example.trade.mapper;

import com.example.trade.entity.Goods;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 10:14
 */
@Mapper
public interface GoodsMapper {

    /**
     * 新增商品。
     * status / view_count 不接受客户端传的值：
     *   status 固定 1（在售），由服务端决定，前端传什么不算数
     *   view_count 固定 0
     * seller_id 和 create_user 也由服务端从 ThreadLocal 取
     */
    @Insert("insert into goods(title,description,price,original_price,cover_img,category_id,seller_id,status,view_count,create_user) " +
            "values (#{title},#{description},#{price},#{originalPrice},#{coverImg},#{categoryId},#{sellerId},1,0,#{createUser})")
    void add(Goods goods);

    /**
     * 商品列表（买家视角）。
     * 三个都是可选条件，为 null 时不拼进 SQL：
     *   categoryId 类目筛选
     *   status     商品状态筛选（1在售 2已售出 3已下架 4待审核）
     *   keyword    关键词，模糊匹配标题和描述
     *
     * 注意参数名统一用 status（对应数据库列名），原来的 state 在库里不存在，
     * MySQL 报 1054 Unknown column 'state'。
     */
    List<Goods> get(@Param("categoryId") Integer categoryId,
                     @Param("status") Integer status,
                     @Param("keyword") String keyword);

    /**
     * 商品详情。
     * 修：原来把浏览量自增写进了 @Select 里（多语句），必须让 JDBC 允许多语句才行。
     * 拆成两个方法：查询归查询，浏览量自增由 Service 单独调用。
     */
    @Select("select * from goods where id = #{id}")
    Goods detail(@Param("id") Long id);

    /** 浏览量 +1 */
    @Update("update goods set view_count = view_count + 1 where id = #{id}")
    void incrViewCount(@Param("id") Long id);

    /**
     * 修改商品。SQL 写在 GoodsMapper.xml 里。
     *
     * 关于参数绑定的坑（很重要，面试常问）：
     *   MyBatis 对**多参数**方法，只要有一个参数加了 @Param，
     *   就会把所有参数装进一个 Map（ParamMap）。
     *   此时实体字段的写法取决于它有没有被 @Param 命名：
     *     - 加了 @Param("goods") → 写 #{goods.title}
     *     - 没加（靠 -parameters 编译参数拿到真实名 goods）→ 同样只能用 #{goods.title}
     *   写成 #{title} 一律报 "Parameter 'title' not found"。
     *   只有**单参数**方法才能直接写 #{title}。
     *
     * SQL 里带 create_user 条件 → 只能改自己发布的商品（防横向越权）。
     */
    void update(@Param("goods") Goods goods, @Param("userId") Integer userId);

    /** 上架 / 下架：只允许改自己发布的商品 */
    @Update("update goods set status=#{status} where id = #{id} and create_user=#{createUser}")
    void status(@Param("createUser") Integer createUser, @Param("id") Long id, @Param("status") Integer status);

    /** 删除：只允许删自己发布的商品 */
    @Delete("delete from goods where id = #{id} and create_user=#{createUser}")
    void delete(@Param("id") Long id, @Param("createUser") Integer createUser);

    /** 我发布的商品（含各种状态，卖家视角） */
    @Select("select * from goods where create_user = #{createUser} order by create_time desc")
    List<Goods> mine(@Param("createUser") Integer createUser);

    /**
     * 下单时把商品改成"已售出"。
     *
     * 关键：加 and status=1 做乐观锁。
     * 如果两个人同时下单同���个商品，第一个执行后 status 变成 2，
     * 第二个再执行时 where 条件不成立 → 影响 0 行 → 事务回滚，不会出现一物二卖。
     *
     * 返回影响行数：1 = 抢到，0 = 已被别人买走
     */
    @Update("update goods set status=2 where id=#{id} and status=1")
    int markSold(@Param("id") Long id);
}
