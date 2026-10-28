package com.example.trade.mapper;

import com.example.trade.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 14:09
 */
@Mapper
public interface OrdersMapper {

    /**
     * 新增订单。
     * 修：列名 buy_id 在表里不存在，真实列名是 buyer_id，MySQL 报 1054。
     * status 由服务端固定为 1（待确认），不接受客户端传值。
     */
    @Insert("insert into orders (order_no,buyer_id,seller_id,goods_id,price,status) " +
            "values (#{orderNo},#{buyerId},#{sellerId},#{goodsId},#{price},1)")
    void add(Orders orders);

    /**
     * 我的订单：role = 'buy' 看我买的，role = 'sell' 看我卖的。
     * 传其他值时不能返回全部订单，所以加了 role 的合法性判断。
     */
    List<Orders> mine(@Param("role") String role, @Param("id") Integer id);

    @Select("select * from orders where id = #{id}")
    Orders detail(@Param("id") Long id);

    /**
     * 确认收货 / 取消订单。
     * 修：原来 where id=#{id} 谁都能改任何人的订单，加买方/卖方归属判断。
     */
    @Update("update orders set status=#{status},update_time=now() " +
            "where id=#{id} and (buyer_id=#{userId} or seller_id=#{userId})")
    int confirm(@Param("status") Integer status, @Param("id") Long id, @Param("userId") Integer userId);
}
