package com.example.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author maofan
 * @create 2026-10-24 20:51
 */
@Data
public class Favorites {
    private Long id;
    private Integer userId;
    private Long goodsId;
    private LocalDateTime createTime;
}
