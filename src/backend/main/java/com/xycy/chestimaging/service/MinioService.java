package com.xycy.chestimaging.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {
    private static final Logger logger = LoggerFactory.getLogger(MinioService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.presigned-url-expiry}")
    private int presignedUrlExpiry;

    @PostConstruct
    public void init() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("[MinIO] 创建存储桶成功: {}", bucketName);
            } else {
                logger.info("[MinIO] 存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("[MinIO] 初始化存储桶失败: {}", e.getMessage(), e);
        }
    }

    public String uploadFile(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            logger.info("[MinIO] 文件上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            logger.error("[MinIO] 文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传到MinIO失败: " + e.getMessage());
        }
    }

    public String uploadFile(InputStream inputStream, long size, String objectName, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            logger.info("[MinIO] 文件上传成功: {}", objectName);
            return objectName;
        } catch (Exception e) {
            logger.error("[MinIO] 文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传到MinIO失败: " + e.getMessage());
        }
    }

    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            logger.error("[MinIO] 文件下载失败: {}", e.getMessage(), e);
            throw new RuntimeException("从MinIO下载文件失败: " + e.getMessage());
        }
    }

    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            logger.info("[MinIO] 文件删除成功: {}", objectName);
        } catch (Exception e) {
            logger.error("[MinIO] 文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("从MinIO删除文件失败: " + e.getMessage());
        }
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedUrlExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            logger.error("[MinIO] 生成预签名URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage());
        }
    }

    public String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "images/" + UUID.randomUUID().toString() + extension;
    }

    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
