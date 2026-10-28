package com.example.trade.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 看图后返回的商品建议信息。
 *
 * 场景：卖家上传一张商品图 → AI 识别 → 自动填标题、描述、建议价、类目、成色。
 * 卖家可以直接采纳，也可以改完再提交。
 *
 * 这个类是"数据载体"，不写任何逻辑。
 * 真正有设计的是 AiServiceImpl（怎么调模型、怎么解析、怎么降级）。
 *
 * @author maofan
 * @create 2026-10-28 09:48
 */
@Data
public class AiSuggestion {

    /** AI 建议的商品标题 */
    private String title;

    /** AI 建议的商品描述 */
    private String description;

    /** AI 建议的售价（模型可能返回 "150元"，解析时已统一转成数字） */
    private BigDecimal price;

    /** AI 识别的类目名（文字，卖家再自己映射到 category_id） */
    private String categoryName;

    /** AI 给出的成色判断，如"九成新" */
    private String condition;

    /** AI 的原始返回文本，排查问题时用 */
    private String rawText;
}
