package com.example.trade.service.impl;

import com.example.trade.entity.AiSuggestion;
import com.example.trade.except.BusinessException;
import com.example.trade.service.AiService;
import com.example.trade.service.OssService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 商品识别实现。
 *
 * 整体流程就三步：
 *   1. 图片传到 OSS，拿到一个公网能访问的地址（模型自己去拉图）
 *   2. 把地址 + 提示词拼成 OpenAI 兼容格式的 JSON，发给百炼
 *   3. 把模型返回的文本抠出 JSON，反射到 AiSuggestion
 *
 * 第 3 步为什么要"抠"：大模型是"生成文本"，不是"返回 JSON"。
 * 它可能给你 "```json\n{...}\n```"，也可能前面带一句"好的，识别结果如下："。
 * 所以不能直接把响应丢给 JSON 解析器，得先剥掉包装。
 *
 * @author maofan
 * @create 2026-10-28 09:46
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    /**
     * 百炼的 OpenAI 兼容端点。
     * 用兼容模式而不是百炼原生模式，是因为原生模式的图片要传 base64，
     * 传公网 URL 更省事、请求体更小。
     */
    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 模型名兜底值。
     *
     * ⚠️ 必须是 qwen3-vl-plus，不是 qwen-vl-plus，也不是 qwen3-v1-plus。
     *    "vl" = vision language（能看图），"v1" 是打错了。
     *    实测：写成 qwen-vl-plus 会返回 "Model not exist"，
     *    因为工作空间专用 Key（sk-ws- 开头）只能调工作空间里部署过的模型。
     *    换 Key 或换工作空间时，先查可用模型：
     *      curl https://dashscope.aliyuncs.com/api/v1/models -H "Authorization: Bearer $KEY"
     */
    private static final String MODEL = "qwen3-vl-plus";

    /**
     * 提示词。
     *
     * 这里的设计有两个要点：
     *   1. 明确说"输出严格 JSON" —— 降低模型返回废话的概率
     *   2. temperature 设 0.2 —— 温度低 = 输出稳定，识别场景不需要发挥
     */
    private static final String PROMPT = """
            你是一个二手交易平台的商品信息助手。请分析这张商品图片，输出严格JSON，格式如下：
            {
                "title": "简洁的商品标题，不超过20字",
                "description": "商品描述，50字以内，客观描述外观、颜色、材质、有无使用痕迹",
                "price": 二手建议售价的数字,
                "categoryName": "商品类目名，如 教材教辅/电子设备/服饰鞋包/生活用品/运动户外",
                "condition": "成色判断，如 全新/九成新/八成新/有明显使用痕迹"
            }
            只输出 JSON，不要任何解释文字。
            """;

    /**
     * API Key 从配置读，配置里写的是 ${DASHSCOPE_API_KEY:}（真实值在环境变量）。
     * 直接写 @Value("${DASHSCOPE_API_KEY:}") 也能读到环境变量，
     * 但那会让"配置在哪"变得不清楚 —— 统一走 application.yml 的 ai.dashscope.* 更好维护。
     */
    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${ai.dashscope.model:qwen3-vl-plus}")
    private String model;

    /**
     * OSS 上传。
     *
     * @Lazy 的原因：OSS 密钥没配时，OssService 初始化不报错，
     * 只有真的去调 uploadFile 才抛"未配置 OSS 密钥"。
     * 不加 @Lazy 的话，AI 模块会把整个应用拖起来都失败 —— 一个可选功能不该拖累主流程。
     */
    @Autowired
    @Lazy
    private OssService ossService;

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传图片 + 识别，一步到位。
     */
    @Override
    public AiSuggestion analyze(MultipartFile file) {
        // 先传 OSS 拿到公网地址 —— 模型必须能访问到图，本地路径它读不到
        String url = ossService.uploadFile(file);
        return analyze(url);
    }

    /**
     * 已有 OSS 地址时直接识别（不重复上传，省一次存储）。
     */
    @Override
    public AiSuggestion analyze(String imageUrl) {
        // 前置校验：这两个失败的话直接给业务异常，别让用户等一次网络超时
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("AI 识别未配置 API Key，请手动填写商品信息");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("图片地址不能为空");
        }

        try {
            Map<String, Object> requestBody = buildRequestBody(imageUrl);
            String response = restClient.post()
                    .uri(DASHSCOPE_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            // 降级：AI 挂了不应该让整个发布流程崩掉，提示用户手填即可。
            // 注意 log.warn 保留原始异常 —— 只抛"AI 识别失败"的话，
            // 线上出问题你根本不知道是超时还是 401。
            log.warn("AI 识别失败：{}", e.getMessage(), e);
            throw new BusinessException("AI 识别失败，请手动填写商品信息");
        }
    }

    /**
     * 组装请求体（OpenAI 兼容格式）。
     *
     * messages 里 content 是一个数组，text 和 image_url 混排 ——
     * 这是多模态的标准写法，模型才知道"哪段是指令、哪段是图"。
     */
    private Map<String, Object> buildRequestBody(String imageUrl) {
        List<Map<String, Object>> content = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", PROMPT);
        content.add(textPart);

        Map<String, Object> imageUrlObj = new HashMap<>();
        imageUrlObj.put("url", imageUrl);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlObj);
        content.add(imagePart);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model == null || model.isBlank() ? MODEL : model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        return body;
    }

    /**
     * 解析返回。
     * 路径：choices[0].message.content 是模型输出的文本（可能带 ```json 包裹）。
     */
    private AiSuggestion parseResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new BusinessException("AI 返回结果为空");
        }
        String text = choices.get(0).path("message").path("content").asText("");
        if (text.isBlank()) {
            throw new BusinessException("AI 未返回内容");
        }

        AiSuggestion suggestion = parseJson(text);
        suggestion.setRawText(text);
        return suggestion;
    }

    /**
     * 从模型输出里抠出 JSON 并解析。
     * 优先找 ```json ... ``` 代码块；找不到就找第一个 { 到最后一个 }。
     */
    private AiSuggestion parseJson(String text) throws Exception {
        String json = text.trim();

        int fenceStart = json.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = json.indexOf('\n', fenceStart);
            int fenceEnd = json.indexOf("```", fenceStart + 3);
            if (contentStart > 0 && fenceEnd > contentStart) {
                json = json.substring(contentStart + 1, fenceEnd).trim();
            }
        }

        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        JsonNode node = objectMapper.readTree(json);

        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setTitle(node.path("title").asText(""));
        suggestion.setDescription(node.path("description").asText(""));
        suggestion.setCategoryName(node.path("categoryName").asText(""));
        suggestion.setCondition(node.path("condition").asText(""));

        // price 可能是数字也可能是字符串（模型常写成 "150元"），都处理一下
        BigDecimal price = parsePrice(node.path("price"));
        suggestion.setPrice(price);
        return suggestion;
    }

    /** 从可能是 150 / "150" / "150元" / "150.5" 的值里提取数字 */
    private BigDecimal parsePrice(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText("");
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(text);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
