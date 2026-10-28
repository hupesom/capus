package com.example.trade.mapper;

import com.example.trade.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 9:41
 */
@Mapper
public interface CategoryMapper {

    @Insert("insert into category(category_name,category_alias,create_user) values (#{categoryName},#{categoryAlias},#{createUser})")
    void add(@Param("categoryName") String categoryName,
             @Param("categoryAlias") String categoryAlias,
             @Param("createUser") Integer createUser);

    /**
     * 类目列表：所有用户都能看到全部类目。
     * 修：原来 where create_user=#{id} 意思是"只能看到自己建的类目"，
     * 但买家需要看到所有类目才能筛选商品 —— 和商品的 create_user 过滤是同一类错误。
     */
    @Select("select * from category order by id")
    List<Category> list();

    @Select("select * from category where id = #{id}")
    Category detail(@Param("id") Long id);

    /**
     * 修改类目。SQL 写在 CategoryMapper.xml。
     *
     * 修两处：
     *   1. 表名 catetory 拼错 → MySQL 报 1146 Table doesn't exist
     *   2. 加上 create_user 条件，防止改别人的类目（横向越权）
     *
     * 为什么放 XML：和 GoodsMapper.update 一样，
     * 这个方法两个参数且有一个带 @Param，注解里就得写 #{category.categoryName}，很别扭。
     */
    void update(Category category, @Param("createUser") Integer createUser);

    /** 删除：修：加上 create_user 条件，防止删别人的类目 */
    @Delete("delete from category where id = #{id} and create_user=#{createUser}")
    void delete(@Param("id") Long id, @Param("createUser") Integer createUser);
}
