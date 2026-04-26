package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.MedicalCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface CaseMapper {
    Optional<MedicalCase> findById(Long id);
    Optional<MedicalCase> findByCaseId(String caseId);
    List<MedicalCase> findAll();
    int insert(MedicalCase medicalCase);
    int update(MedicalCase medicalCase);
    int deleteById(Long id);
    long count();
    List<MedicalCase> findByCondition(@Param("caseId") String caseId, @Param("patientName") String patientName,
                                      @Param("offset") int offset, @Param("pageSize") int pageSize);
    long countByCondition(@Param("caseId") String caseId, @Param("patientName") String patientName);

    void updateCaseId(@Param("id") Long id);

    List<Map<String, Object>> findCasesWithImageCount(@Param("caseId") String caseId, @Param("patientName") String patientName,
                                                      @Param("offset") int offset, @Param("pageSize") int pageSize);
    long countCasesWithImageCount(@Param("caseId") String caseId, @Param("patientName") String patientName);
    
    long countByStatus(@Param("status") String status);
}