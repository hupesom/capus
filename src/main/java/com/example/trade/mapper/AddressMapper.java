package com.example.trade.mapper;

import com.example.trade.entity.Address;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-27 20:18
 */
@Mapper
public interface AddressMapper {
    @Insert("insert into address(user_id,receiver,phone,address,is_default) values (#{userId},#{receiver},#{phone},#{address},#{isDefault})")
    void add(Address address);
    @Select("select * from address where user_id=#{userId} order by is_default desc,id desc")
    List<Address> list(@Param("userId")Integer userId);
    @Select("select * from address where user_id=#{userId} and id=#{id}")
    Address detail(@Param("id")Long id,@Param("userId")Integer userId);
    @Update("update address set receiver=#{receiver},phone=#{phone},address=#{address}," +
            "update_time=now() where id=#{id} and user_id=#{userId}")
    void update(Address address);

    @Delete("delete from address where id=#{id} and user_id=#{userId}")
    void delete(@Param("id") Long id, @Param("userId") Integer userId);

    /** 设默认地址：先把该用户其他地址都取消默认，再设这一个 */
    @Update("update address set is_default=0 where user_id=#{userId}")
    void clearDefault(@Param("userId") Integer userId);

    @Update("update address set is_default=1 where id=#{id} and user_id=#{userId}")
    void setDefault(@Param("id") Long id, @Param("userId") Integer userId);

    @Select("select * from address where id=#{id}")
    Address getById(@Param("id") Long id);
}

















