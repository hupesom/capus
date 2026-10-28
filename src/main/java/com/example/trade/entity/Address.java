package com.example.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author maofan
 * @create 2026-10-24 20:48
 */
@Data
public class Address {
    private Long id;
    private Integer userId;
    private String receiver;
    private String phone;
    private String address;
    private String isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
