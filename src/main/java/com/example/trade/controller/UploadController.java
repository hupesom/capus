package com.example.trade.controller;

import com.example.trade.entity.Result;
import com.example.trade.service.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author maofan
 * @create 2026-10-26 15:07
 */
//不会写，但是能看懂，文件上传三个文件都是找deepseek问的
@RestController
@RequestMapping("upload")
public class UploadController {
    @Autowired
    private OssService ossService;
    @PostMapping
    public Result<String> upload(@RequestParam("file")MultipartFile file){
        String url = ossService.uploadFile(file);
        return Result.success(url);
    }
}
