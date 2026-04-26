package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.cases.CaseCreateRequest;
import com.xycy.chestimaging.dto.cases.CaseListResponse;
import com.xycy.chestimaging.dto.cases.CaseResponse;
import com.xycy.chestimaging.dto.cases.CaseUpdateRequest;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.exception.NotFoundException;
import com.xycy.chestimaging.mapper.CaseMapper;
import com.xycy.chestimaging.model.MedicalCase;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.service.CaseService;
import com.xycy.chestimaging.service.DetectionService;
import com.xycy.chestimaging.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CaseServiceImpl implements CaseService {
    private static final Logger logger = LoggerFactory.getLogger(CaseServiceImpl.class);
    @Autowired
    private CaseMapper caseMapper;
    @Autowired
    private CacheService cacheService;
    @Autowired
    @Lazy
    private ImageService imageService;

    @Autowired
    private DetectionService detectionService;

    @Override
    public PaginationResponse<CaseListResponse> getCases(int page, int pageSize, String caseId, String patientName) {
        PaginationResponse<CaseListResponse> cachedResult = cacheService.getCaseListCache(page, pageSize, caseId, patientName);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        logger.info("[病例查询] 缓存未命中，从数据库查询: page={}, size={}, caseId={}, patientName={}", page, pageSize, caseId, patientName);
        int offset = (page - 1) * pageSize;
        
        List<MedicalCase> cases = caseMapper.findByCondition(caseId, patientName, offset, pageSize);
        long total = caseMapper.countByCondition(caseId, patientName);
        
        List<CaseListResponse> caseListResponses = cases.stream()
                .map(caseEntity -> {
                    int imageCount = imageService.getImageCountByCaseId(caseEntity.getId());
                    return new CaseListResponse(
                            caseEntity.getId(),
                            caseEntity.getCaseId(),
                            caseEntity.getPatientName(),
                            caseEntity.getCreatedAt(),
                            imageCount,
                            caseEntity.getStatus()
                    );
                })
                .collect(Collectors.toList());

        PaginationResponse<CaseListResponse> result = new PaginationResponse<>(total, caseListResponses);
        cacheService.cacheCaseList(page, pageSize, caseId, patientName, result);
        return result;
    }

    @Override
    public CaseResponse createCase(CaseCreateRequest request, String username) {
        MedicalCase caseEntity = new MedicalCase();
        caseEntity.setPatientName(request.getPatientName());
        caseEntity.setPatientIdCard(request.getPatientIdCard());
        caseEntity.setPatientPhone(request.getPatientPhone());
        if (request.getPatientBirthday() != null) {
            caseEntity.setPatientBirthday(request.getPatientBirthday().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }
        caseEntity.setPatientGender(request.getPatientGender());
        caseEntity.setCaseDescription(request.getCaseDescription());
        if (request.getCheckDate() != null) {
            caseEntity.setCheckDate(request.getCheckDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }
        caseEntity.setCheckNote(request.getCheckNote());
        caseEntity.setStatus("待上传影像");
        caseEntity.setCreatedAt(LocalDateTime.now());
        caseEntity.setCreatedBy(username);

        caseMapper.insert(caseEntity);
        caseMapper.updateCaseId(caseEntity.getId());
        
        if (caseEntity.getId() != null) {
            caseEntity.setCaseId(String.valueOf(caseEntity.getId()));
            caseMapper.update(caseEntity);
            logger.info("[病例创建] 病例ID: {} 业务编号设置为: {}", caseEntity.getId(), caseEntity.getCaseId());
        }
        
        cacheService.evictAllCaseListCache();
        
        return new CaseResponse(caseEntity);
    }

    @Override
    public CaseResponse getCaseById(Long id) {
        CaseResponse cachedResult = cacheService.getCaseDetailCache(id);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        logger.info("[病例查询] 缓存未命中，从数据库查询: id={}", id);
        Optional<MedicalCase> optionalCase = caseMapper.findById(id);
        if (!optionalCase.isPresent()) {
            throw new NotFoundException("病例不存在");
        }
        CaseResponse caseResponse = new CaseResponse(optionalCase.get());
        List<DetectionResponse> detections = detectionService.getDetectionsByCaseId(id);
        caseResponse.setDetections(detections);
        
        cacheService.cacheCaseDetail(id, caseResponse);
        return caseResponse;
    }

    @Override
public CaseResponse updateCase(Long id, CaseUpdateRequest request) {
    Optional<MedicalCase> optionalCase = caseMapper.findById(id);
    if (!optionalCase.isPresent()) {
        throw new NotFoundException("病例不存在");
    }

    MedicalCase caseEntity = optionalCase.get();

    if (request.getPatientName() != null) {
        caseEntity.setPatientName(request.getPatientName());
    }

    if (request.getPatientGender() != null) {
        caseEntity.setPatientGender(request.getPatientGender());
    }

    if (request.getPatientIdCard() != null) {
        caseEntity.setPatientIdCard(request.getPatientIdCard());
    }

    if (request.getPatientPhone() != null) {
        caseEntity.setPatientPhone(request.getPatientPhone());
    }

    if (request.getPatientBirthday() != null) {
        caseEntity.setPatientBirthday(request.getPatientBirthday());
    }

    if (request.getCaseDescription() != null) {
        caseEntity.setCaseDescription(request.getCaseDescription());
    }

    if (request.getCheckDate() != null) {
        caseEntity.setCheckDate(request.getCheckDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }

    if (request.getCheckNote() != null) {
        caseEntity.setCheckNote(request.getCheckNote());
    }

    caseMapper.update(caseEntity);
    
    cacheService.evictCaseDetailCache(id);
    cacheService.evictAllCaseListCache();
    
    return new CaseResponse(caseEntity);
}

    @Override
    public void deleteCase(Long id) {
        Optional<MedicalCase> optionalCase = caseMapper.findById(id);
        if (!optionalCase.isPresent()) {
            throw new NotFoundException("病例不存在");
        }
        caseMapper.deleteById(id);
        cacheService.evictCaseDetailCache(id);
        cacheService.evictAllCaseListCache();
    }

    @Override
    public void updateCaseStatus(Long caseId, String status) {
        Optional<MedicalCase> optionalCase = caseMapper.findById(caseId);
        if (optionalCase.isPresent()) {
            MedicalCase caseEntity = optionalCase.get();
            String oldStatus = caseEntity.getStatus();
            caseEntity.setStatus(status);
            caseMapper.update(caseEntity);
            logger.info("[状态更新] 病例ID: {} 状态从 '{}' 变更为 '{}'", caseId, oldStatus, status);
        } else {
            logger.error("[状态更新] 病例ID: {} 不存在，无法更新状态为 '{}'", caseId, status);
        }
    }
}
