package com.example.trade.conf;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * OSS 客户端配置。
 *
 * 修：原来写的是 ${aliyun.oss.enpoint}（少了个 d），而 application.yml 里的 key 是
 * endpoint。占位符解析不到 → 应用启动直接抛
 * "Could not resolve placeholder 'aliyun.oss.enpoint'"。
 *
 * 关键设计：加了 @Lazy
 *   之前 OSSClient 是启动时就创建的 Bean，如果没配 AccessKey，
 *   整个应用直接起不来 —— 连商品列表、订单这些跟 OSS 毫无关系的接口都用不了。
 *   加了 @Lazy 之后，只有第一次真要上传文件时才创建客户端，
 *   没配 Key 也不影响其他功能（只有上传接口会报错）。
 *
 *   这就是"惰性初始化"，面试可以讲：可选依赖不要拖累主流程。
 *
 * @author maofan
 * @create 2026-10-26 15:24
 */
@Configuration
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId:}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret:}")
    private String accessKeySecret;

    /**
     * OSSClient 做成单例 Bean（不是每次 new）。
     * 每次 new 会新建连接池，高并发下连接数暴涨。
     * destroyMethod = "shutdown" 保证容器关闭时释放连接。
     */
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
