package com.xycy.chestimaging.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtils {
    private static final String UPLOAD_DIR =  Paths.get(System.getProperty("user.dir"), "uploads").toString();

    public String saveFile(MultipartFile file, Long caseId) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(UPLOAD_DIR, caseId.toString());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
        String fileName = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath);

        // 返回相对路径
        return caseId + "/" + fileName;
    }

    public void deleteFile(String relativePath) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR, relativePath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    public File getFile(String relativePath) {
        Path filePath = Paths.get(UPLOAD_DIR, relativePath);
        return filePath.toFile();
    }
}