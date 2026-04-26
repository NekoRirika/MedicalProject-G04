package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.image.BatchUploadResult;
import com.xycy.chestimaging.dto.image.ImageResponse;
import com.xycy.chestimaging.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {
    List<ImageResponse> getImagesByCaseId(Long caseId);
    ImageResponse getImageById(Long imageId);
    Image getImageEntityById(Long imageId);
    ImageResponse uploadImage(Long caseId, MultipartFile file, String username) throws IOException;
    BatchUploadResult batchUploadImages(Long caseId, List<MultipartFile> files, String username) throws IOException;
    void deleteImage(Long caseId, Long imageId) throws IOException;
    int getImageCountByCaseId(Long caseId);
}
