package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.Model;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ModelMapper {
    Optional<Model> findById(Long id);
    Optional<Model> findActiveModel();
    List<Model> findAll();
    int insert(Model model);
    int update(Model model);
    int deactivateAll();
    int deleteById(Long id);
    long count();
    List<Model> findByNameAndVersion(String name, String version);
    int updateAccuracy(@Param("id") Long id, @Param("accuracy") Double accuracy);
}