package com.museum.controller;

import com.museum.config.JsonResult;
import com.museum.config.MinioConfig;
import com.museum.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @since 2023-12-19
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    
    private final MinioUtils minioUtils;
    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    @PostMapping("/uploadFile")
    public JsonResult uploadFile(@RequestParam("file") MultipartFile[] file) {
        if(file == null || file.length < 1) {
            return JsonResult.failResult("文件上传失败");
        }
        if (file[0].isEmpty()){
            return JsonResult.failResult("文件上传失败");
        }
        
        String fileName = file[0].getOriginalFilename();
        String suffixName = fileName.substring(fileName.lastIndexOf("."));
        
        // 生成文件名称通用方法
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Random r = new Random();
        StringBuilder tempName = new StringBuilder();
        tempName.append(sdf.format(new Date())).append(r.nextInt(100)).append(suffixName);
        String objectName = tempName.toString();
        
        try {
            // 获取文件MIME类型
            String contentType = file[0].getContentType();
            
            // 上传到MinIO
            minioUtils.uploadFile(
                    minioConfig.getBucketName(), 
                    objectName, 
                    file[0].getInputStream(),
                    contentType
            );
            
            return JsonResult.result(objectName);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.failResult("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/getPic")
    public void getPic(String name, HttpServletResponse response) {
        try {
            // 生成有效期为7天的预签名URL
            String presignedUrl = minioUtils.getPresignedUrl(
                    minioConfig.getBucketName(), 
                    name, 
                    7 * 24 * 60 * 60  // 7天的秒数
            );
            
            response.sendRedirect(presignedUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
