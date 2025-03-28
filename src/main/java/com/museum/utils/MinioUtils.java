package com.museum.utils;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;

    /**
     * 上传文件到MinIO
     */
    public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) 
            throws Exception {
        // 检查存储桶是否存在
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            // 创建存储桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        
        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType(contentType)
                        .build());
    }

    /**
     * 获取文件临时访问URL
     */
    public String getPresignedUrl(String bucketName, String objectName, int expiryTimeInSeconds) 
            throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiryTimeInSeconds, TimeUnit.SECONDS)
                        .build());
    }
} 