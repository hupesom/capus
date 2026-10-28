package com.example.trade.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
@Data
public class User {
    private Integer id;//主键ID
    @Size(min = 5,max = 16)
    @NotBlank
    private String username;//用户名
    @JsonIgnore
    @Size(min = 5,max = 16)
    private String password;//密码
    @Size(min = 1,max = 10)
    @NotBlank
    private String nickname;//昵称
    @Email
    private String email;//邮箱
    private String userPic;//用户头像地址
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
}
