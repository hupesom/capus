package com.example.trade.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.example.trade.except.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传。
 *
 * @author maofan
 * @create 2026-10-26 15:24
 */
@Service
public class OssService {

    /**
     * 用 @Lazy 注入：没配 AccessKey 时，整个应用照样能启动、
     * 其他接口照样能用，只有调用上传接口时才报错。
     */
    @Autowired
    @Lazy
    private OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId:}")
    private String accessKeyId;

    /**
     * 上传文件，返回公网访问地址。
     *
     * 三个关键点：
     * 1. 文件名用 UUID 重新生成 —— 用原名会互相覆盖，中文名在 URL 里还会乱码
     * 2. 必须设 Content-Type —— 不设的话浏览器会当成未知二进制，下载而不是显示图片
     * 3. 返回 URL 时要去掉 endpoint 里的 https:// 前缀，否则拼成
     *    "https://testossmf.https://oss-cn-beijing.aliyuncs.com/xxx" —— 打不开
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 没配密钥时给一句人话，而不是抛阿里云 SDK 的 InvalidCredentialsException
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new BusinessException("文件上传未配置 OSS 密钥，请设置环境变量 ALIYUN_OSS_ID 和 ALIYUN_OSS_SECRET");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > -1) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(getContentType(ext));

            ossClient.putObject(bucketName, objectName, inputStream, objectMetadata);
            return buildUrl(objectName);
        } catch (Exception e) {
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }

    /** 根据后缀判断 MIME 类型，决定浏览器怎么渲染 */
    private String getContentType(String ext) {
        String lower = ext.toLowerCase(Locale.ROOT);
        switch (lower) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".bmp":
                return "image/bmp";
            default:
                return "application/octet-stream";
        }
    }

    /** 拼公网 URL：https://{bucket}.{host}/{objectName} */
    private String buildUrl(String objectName) {
        String host = endpoint;
        if (host.startsWith("https://")) {
            host = host.substring("https://".length());
        } else if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        }
        return "https://" + bucketName + "." + host + "/" + objectName;
    }
}
