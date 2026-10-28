package com.example.trade.entity;

import lombok.Data;

import java.util.List;

/**
 * @author maofan
 * @create 2026-10-26 11:06
 */
@Data
public class PageBean<T> {
    private Integer total;
    private List<T> items;
}
