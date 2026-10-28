package com.example.trade.entity;

import com.example.trade.status.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author maofan
 * @create 2026-10-24 21:12
 */
@Data
public class Orders {
    private Long id;
    private String orderNo;
    private Integer buyerId;
    private Integer sellerId;
    private Long goodsId;
    private BigDecimal price;
    @Status(values = {1,2,3},message = "订单状态只能为1(待确认)2(已完成)3(已取消)")
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
