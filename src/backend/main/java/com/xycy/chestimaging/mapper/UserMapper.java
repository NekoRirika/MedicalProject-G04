package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    int insert(User user);
    int update(User user);
    int deleteById(Long id);
    long count();
    List<User> findByCondition(@Param("username") String username, @Param("department") String department, @Param("role") String role,
                               @Param("status") String status, @Param("offset") int offset, @Param("pageSize") int pageSize);
    long countByCondition(@Param("username") String username, @Param("department") String department, @Param("role") String role, @Param("status") String status);
}