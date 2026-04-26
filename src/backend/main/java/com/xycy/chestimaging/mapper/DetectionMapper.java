package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.Detection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DetectionMapper {
    Optional<Detection> findById(Long id);
    List<Detection> findByCaseId(Long caseId);
    List<Detection> findAll();
    int insert(Detection detection);
    int update(Detection detection);
    int deleteById(Long id);
    long count();
    long countByDate(@Param("date") java.time.LocalDate date);
    long countCompletedByDate(@Param("date") java.time.LocalDate date);
    List<Detection> findByModelId(@Param("modelId") Long modelId);
}