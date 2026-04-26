package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface FeedbackMapper {
    Optional<Feedback> findById(Long id);
    List<Feedback> findByDetectionId(Long detectionId);
    List<Feedback> findAll();
    int insert(Feedback feedback);
    int update(Feedback feedback);
    int deleteById(Long id);
    long count();

    List<Map<String,Object>> findAllWithDetails(@Param("offset") int offset, @Param("limit") int limit);
    long countAll();
}