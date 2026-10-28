package com.example.trade.entity;

import lombok.Data;

import java.io.PipedReader;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author maofan
 * @create 2026-10-24 20:50
 */
@Data
public class Category {
    private Long id;
    private String categoryName;
    private String categoryAlias;
    private Integer createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
