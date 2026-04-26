package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ImageMapper {
    Optional<Image> findById(Long id);
    List<Image> findByCaseId(Long caseId);
    List<Image> findAll();
    int insert(Image image);
    int update(Image image);
    int deleteById(Long id);
    int deleteByCaseId(Long caseId);
    long count();
    int countByCaseId(Long caseId);
    long countByDate(@Param("date") java.time.LocalDate date);
}