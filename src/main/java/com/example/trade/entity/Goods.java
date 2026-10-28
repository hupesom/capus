package com.example.trade.entity;

import com.example.trade.status.Status;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体。
 *
 * 加了参数校验注解，但注意一个前提：
 * 这些字段上的注解只有在 Controller 参数上写了 @Valid 时才会执行。
 * 只写 @Validated（类上）只对普通参数生效，对 @RequestBody 对象必须用 @Valid。
 *
 * @author maofan
 * @create 2026-10-24 21:08
 */
@Data
public class Goods {
    private Long id;

    /** 商品标题，必填 */
    @NotBlank(message = "商品标题不能为空")
    private String title;

    /** 商品描述 */
    private String description;

    /** 售价，必填且大于 0。用 BigDecimal 而不是 double，double 存金额有精度损失 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    /** 原价 / 参考价 */
    private BigDecimal originalPrice;

    /** 商品主图（OSS 地址） */
    private String coverImg;

    /** 类目 id，必填 */
    @NotNull(message = "请选择商品类目")
    private Long categoryId;

    /** 卖家 id：服务端从 ThreadLocal 取，客户端传了也不认 */
    private Integer sellerId;

    /** 商品状态：1在售 2已售出 3已下架 4待审核。服务端控制，不接受客户端设置 */
    @Status(values = {1, 2, 3, 4})
    private Integer status;

    /** 浏览量：服务端维护 */
    private Integer viewCount;

    /** 发布人 id：服务端从 ThreadLocal 取 */
    private Integer createUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
