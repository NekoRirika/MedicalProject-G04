package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AuditLogMapper {
    Optional<AuditLog> findById(Long id);
    List<AuditLog> findAll();
    int insert(AuditLog auditLog);
    int update(AuditLog auditLog);
    int deleteById(Long id);
    long count();
    List<AuditLog> findByCondition(@Param("operator") String operator, @Param("operationType") String operationType,
                                   @Param("startTime") String startTime, @Param("endTime") String endTime,
                                   @Param("offset") int offset, @Param("pageSize") int pageSize);
    long countByCondition(@Param("operator") String operator, @Param("operationType") String operationType,
                          @Param("startTime") String startTime, @Param("endTime") String endTime);
}
