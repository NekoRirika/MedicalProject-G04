package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.annotation.AuditLog;
import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.image.BatchUploadResult;
import com.xycy.chestimaging.exception.AccessDeniedException;
import com.xycy.chestimaging.exception.NotFoundException;
import com.xycy.chestimaging.model.Image;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.ImageService;
import com.xycy.chestimaging.service.UserService;
import com.xycy.chestimaging.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/cases/{caseId}/images")
public class ImageController {
    @Autowired
    private ImageService imageService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileUtils fileUtils;

    private void checkDoctorRole() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user.getRole() != User.Role.doctor) {
            throw new AccessDeniedException("权限不足，只有医生可以操作");
        }
    }

    private void checkAuth() {
        SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    @AuditLog(operationType = "QUERY_IMAGE", operationContent = "查询影像列表")
    public Response<?> getImagesByCaseId(@PathVariable Long caseId) {
        checkAuth();
        return Response.success("查询成功", imageService.getImagesByCaseId(caseId));
    }

    @GetMapping("/{imageId}/file")
    @AuditLog(operationType = "DOWNLOAD_IMAGE", operationContent = "下载影像文件")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long caseId, @PathVariable Long imageId) {
        checkAuth();
        Image image = imageService.getImageEntityById(imageId);
        
        if (!image.getCaseId().equals(caseId)) {
            throw new NotFoundException("影像不属于该病例");
        }
        
        File file = fileUtils.getFile(image.getFilePath());
        if (!file.exists()) {
            throw new NotFoundException("文件不存在");
        }
        
        Resource resource = new FileSystemResource(file);
        String contentType = getContentType(image.getFileName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + URLEncoder.encode(image.getFileName(), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }

    @GetMapping("/{*filePath}")
    public ResponseEntity<Resource> getImageByPath(@PathVariable Long caseId, @PathVariable String filePath) {
        checkAuth();
        String cleanFilePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        
        File file = fileUtils.getFile(cleanFilePath);
        if (!file.exists()) {
            throw new NotFoundException("文件不存在");
        }
        
        Resource resource = new FileSystemResource(file);
        String contentType = getContentType(file.getName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @PostMapping
    @AuditLog(operationType = "UPLOAD_IMAGE", operationContent = "上传影像文件")
    public Response<?> uploadImage(@PathVariable Long caseId, @RequestParam("file") MultipartFile file) {
        checkDoctorRole();
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return Response.success("上传成功", imageService.uploadImage(caseId, file, username));
        } catch (IOException e) {
            return Response.error(500, "上传失败", "文件保存失败");
        }
    }

    @PostMapping("/batch")
    @AuditLog(operationType = "BATCH_UPLOAD_IMAGE", operationContent = "批量上传影像文件")
    public Response<?> batchUploadImages(@PathVariable Long caseId, @RequestParam("files") List<MultipartFile> files) {
        checkDoctorRole();
        if (files == null || files.isEmpty()) {
            return Response.error(400, "上传失败", "请选择要上传的文件");
        }
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            BatchUploadResult result = imageService.batchUploadImages(caseId, files, username);
            String message = String.format("批量上传完成，成功%d个，失败%d个", result.getSuccessCount(), result.getFailedCount());
            return Response.success(message, result);
        } catch (IOException e) {
            return Response.error(500, "上传失败", "文件保存失败");
        }
    }

    @DeleteMapping("/{imageId}")
    @AuditLog(operationType = "DELETE_IMAGE", operationContent = "删除影像文件")
    public Response<?> deleteImage(@PathVariable Long caseId, @PathVariable Long imageId) {
        checkDoctorRole();
        try {
            imageService.deleteImage(caseId, imageId);
            return Response.success("删除成功");
        } catch (IOException e) {
            return Response.error(500, "删除失败", "文件删除失败");
        }
    }
    
    private String getContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFileName.endsWith(".dcm")) {
            return "application/dicom";
        }
        return "application/octet-stream";
    }
}
