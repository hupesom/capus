package com.example.trade.entity;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * @author maofan
 * @create 2026-10-25 22:04
 */
@Data
public class UserUpdateDTO {
    @URL(message = "头像必须是个合法网址")
    private String avatarUrl;
}
