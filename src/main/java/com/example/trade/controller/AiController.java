package com.example.trade.controller;

import com.example.trade.entity.AiSuggestion;
import com.example.trade.entity.Result;
import com.example.trade.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 商品识别接口。
 *
 * @author maofan
 * @create 2026-10-28 09:44
 */
@RestController
@RequestMapping("ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * 上传商品图，AI 识别出标题/描述/建议价/类目/成色。
     *
     * 表单字段名必须是 file（和 MultipartFile 参数名一致）。
     */
    @PostMapping("goods/analyze")
    public Result<AiSuggestion> analyze(@RequestParam("file") MultipartFile file) {
        AiSuggestion suggestion = aiService.analyze(file);
        return Result.success(suggestion);
    }

    /**
     * 已经有 OSS 地址时，直接分析（不重复上传，省一次存储）。
     */
    @PostMapping("goods/analyze/url")
    public Result<AiSuggestion> analyzeByUrl(@RequestParam("imageUrl") String imageUrl) {
        AiSuggestion suggestion = aiService.analyze(imageUrl);
        return Result.success(suggestion);
    }
}
