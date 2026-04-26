package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.dashboard.DashboardStats;
import com.xycy.chestimaging.mapper.CaseMapper;
import com.xycy.chestimaging.mapper.DetectionMapper;
import com.xycy.chestimaging.mapper.ImageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    @Autowired
    private CaseMapper caseMapper;
    @Autowired
    private ImageMapper imageMapper;
    @Autowired
    private DetectionMapper detectionMapper;

    @GetMapping("/stats")
    public Response<?> getDashboardStats() {
        LocalDate today = LocalDate.now();
        
        long pendingCases = caseMapper.countByStatus("待检测") + caseMapper.countByStatus("待上传影像");
        long todayImages = imageMapper.countByDate(today);
        long todayDetections = detectionMapper.countByDate(today);
        long completedDetections = detectionMapper.countCompletedByDate(today);
        
        double detectionAccuracy = todayDetections > 0 ? (double) completedDetections / todayDetections * 100 : 0;
        
        DashboardStats stats = new DashboardStats();
        stats.setPendingCases(pendingCases);
        stats.setTodayImages(todayImages);
        stats.setTodayDetections(todayDetections);
        stats.setDetectionAccuracy(Math.round(detectionAccuracy * 10.0) / 10.0);
        
        return Response.success("获取成功", stats);
    }
}
