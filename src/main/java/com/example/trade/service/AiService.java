package com.example.trade.service;

import com.example.trade.entity.AiSuggestion;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 商品识别服务。
 *
 * @author maofan
 * @create 2026-10-28 09:45
 */
public interface AiService {

    /**
     * 看图生成商品信息。
     *
     * @param imageUrl 图片的可公网访问地址（OSS 地址）。
     *                多模态模型是"自己去拉图"，所以必须是模型能访问到的公网地址，
     *                本地路径 / 相对路径模型都读不到。
     */
    AiSuggestion analyze(String imageUrl);

    /**
     * 上传图片 + 识别，一步到位。
     *
     * 内部先把文件传 OSS 拿到公网地址，再调 analyze(String)。
     */
    AiSuggestion analyze(MultipartFile file);
}
