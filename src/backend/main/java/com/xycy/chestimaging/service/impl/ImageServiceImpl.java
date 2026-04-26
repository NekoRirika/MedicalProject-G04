package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.dto.image.BatchUploadResult;
import com.xycy.chestimaging.dto.image.ImageResponse;
import com.xycy.chestimaging.exception.FileTypeException;
import com.xycy.chestimaging.exception.NotFoundException;
import com.xycy.chestimaging.mapper.ImageMapper;
import com.xycy.chestimaging.model.Image;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.service.CaseService;
import com.xycy.chestimaging.service.ImageService;
import com.xycy.chestimaging.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ImageServiceImpl implements ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);
    @Autowired
    private ImageMapper imageMapper;
    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private CacheService cacheService;
    @Autowired
    @Lazy
    private CaseService caseService;

    @Override
    public List<ImageResponse> getImagesByCaseId(Long caseId) {
        List<ImageResponse> cachedResult = cacheService.getImageListCache(caseId);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        logger.info("[影像查询] 缓存未命中，从数据库查询: caseId={}", caseId);
        caseService.getCaseById(caseId);
        
        List<Image> images = imageMapper.findByCaseId(caseId);
        List<ImageResponse> result = images.stream()
                .map(ImageResponse::new)
                .collect(Collectors.toList());
        
        cacheService.cacheImageList(caseId, result);
        return result;
    }

    @Override
    public ImageResponse uploadImage(Long caseId, MultipartFile file, String username) throws IOException {
        caseService.getCaseById(caseId);
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "dcm"};
        boolean isImageFile = false;
        for (String extension : allowedExtensions) {
            if (fileExtension.equals(extension)) {
                isImageFile = true;
                break;
            }
        }
        
        if (!isImageFile) {
            throw new FileTypeException("只允许上传影像文件（jpg、jpeg、png、gif、bmp、dcm）");
        }

        String relativePath = fileUtils.saveFile(file, caseId);

        Image image = new Image();
        image.setCaseId(caseId);
        image.setFileName(file.getOriginalFilename());
        image.setFilePath(relativePath);
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());
        image.setUploadedBy(username);

        imageMapper.insert(image);

        cacheService.evictImageListCache(caseId);
        cacheService.evictCaseDetailCache(caseId);
        
        logger.info("[状态更新] 病例ID: {} 状态从 '待上传影像' 变更为 '待检测'", caseId);
        caseService.updateCaseStatus(caseId, "待检测");

        return new ImageResponse(image);
    }

    @Override
    public void deleteImage(Long caseId, Long imageId) throws IOException {
        Optional<Image> optionalImage = imageMapper.findById(imageId);
        if (!optionalImage.isPresent()) {
            throw new NotFoundException("影像不存在");
        }

        Image image = optionalImage.get();
        if (!image.getCaseId().equals(caseId)) {
            throw new RuntimeException("影像不属于该病例");
        }

        fileUtils.deleteFile(image.getFilePath());

        imageMapper.deleteById(imageId);

        cacheService.evictImageListCache(caseId);
        cacheService.evictCaseDetailCache(caseId);

        int imageCount = getImageCountByCaseId(caseId);
        if (imageCount == 0) {
            logger.info("[状态更新] 病例ID: {} 状态从 '待检测' 变更为 '待上传影像'", caseId);
            caseService.updateCaseStatus(caseId, "待上传影像");
        }
    }

    @Override
    public int getImageCountByCaseId(Long caseId) {
        return imageMapper.countByCaseId(caseId);
    }

    @Override
    public ImageResponse getImageById(Long imageId) {
        Image image = getImageEntityById(imageId);
        return new ImageResponse(image);
    }

    @Override
    public Image getImageEntityById(Long imageId) {
        Optional<Image> optionalImage = imageMapper.findById(imageId);
        if (!optionalImage.isPresent()) {
            throw new NotFoundException("影像不存在");
        }
        return optionalImage.get();
    }

    @Override
    @Transactional
    public BatchUploadResult batchUploadImages(Long caseId, List<MultipartFile> files, String username) throws IOException {
        caseService.getCaseById(caseId);
        
        BatchUploadResult result = new BatchUploadResult();
        result.setTotalCount(files.size());
        
        List<ImageResponse> successImages = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                ImageResponse imageResponse = uploadImage(caseId, file, username);
                successImages.add(imageResponse);
            } catch (Exception e) {
                logger.warn("[批量上传] 文件上传失败: {}, 原因: {}", file.getOriginalFilename(), e.getMessage());
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        
        result.setSuccessCount(successImages.size());
        result.setFailedCount(failedFiles.size());
        result.setSuccessImages(successImages);
        result.setFailedFiles(failedFiles);
        
        logger.info("[批量上传] 完成: caseId={}, 总数={}, 成功={}, 失败={}", 
                    caseId, result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
        
        return result;
    }
}
