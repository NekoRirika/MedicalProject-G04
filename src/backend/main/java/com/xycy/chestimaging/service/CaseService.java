package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.cases.CaseCreateRequest;
import com.xycy.chestimaging.dto.cases.CaseListResponse;
import com.xycy.chestimaging.dto.cases.CaseResponse;
import com.xycy.chestimaging.dto.cases.CaseUpdateRequest;


public interface CaseService {
    PaginationResponse<CaseListResponse> getCases(int page, int pageSize, String caseId, String patientName);
    CaseResponse createCase(CaseCreateRequest request, String username);
    CaseResponse getCaseById(Long id);
    CaseResponse updateCase(Long id, CaseUpdateRequest request);
    void deleteCase(Long id);
    void updateCaseStatus(Long caseId, String status);
}
