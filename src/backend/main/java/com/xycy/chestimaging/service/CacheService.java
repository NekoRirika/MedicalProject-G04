package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.cases.CaseListResponse;
import com.xycy.chestimaging.dto.image.ImageResponse;
import com.xycy.chestimaging.dto.detection.DetectionResponse;
import com.xycy.chestimaging.dto.cases.CaseResponse;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final Random RANDOM = new Random();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_INFO_KEY_PREFIX = "user:info:";
    private static final String MODEL_INFO_KEY_PREFIX = "model:info:";
    private static final String USER_SESSION_KEY_PREFIX = "user:session:";
    private static final String CASE_LIST_KEY_PREFIX = "case:list:page:";
    private static final String IMAGE_LIST_KEY_PREFIX = "image:list:case:";
    private static final String DETECTION_RESULT_KEY_PREFIX = "detection:result:";
    private static final String CASE_DETAIL_KEY_PREFIX = "case:detail:";
    private static final String NULL_VALUE_MARKER = "__NULL__";
    private static final long USER_INFO_TTL = 30;
    private static final long MODEL_INFO_TTL = 60;
    private static final long USER_SESSION_TTL = 24;
    private static final long CASE_LIST_TTL = 5;
    private static final long IMAGE_LIST_TTL = 10;
    private static final long DETECTION_RESULT_TTL = 24;
    private static final long CASE_DETAIL_TTL = 10;
    private static final long NULL_CACHE_TTL = 1;

    public void cacheUserInfo(String username, User user) {
        String key = USER_INFO_KEY_PREFIX + username;
        try {
            redisTemplate.opsForValue().set(key, user, USER_INFO_TTL, TimeUnit.MINUTES);
            logger.debug("用户信息已缓存: {}", username);
        } catch (Exception e) {
            logger.error("缓存用户信息失败: {}", username, e);
        }
    }

    public User getUserInfo(String username) {
        String key = USER_INFO_KEY_PREFIX + username;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                logger.debug("从缓存获取用户信息: {}", username);
                return (User) obj;
            }
        } catch (Exception e) {
            logger.error("获取缓存用户信息失败: {}", username, e);
        }
        return null;
    }

    public void evictUserInfo(String username) {
        String key = USER_INFO_KEY_PREFIX + username;
        try {
            redisTemplate.delete(key);
            logger.debug("用户信息缓存已删除: {}", username);
        } catch (Exception e) {
            logger.error("删除用户信息缓存失败: {}", username, e);
        }
    }

    public void cacheModelInfo(Long modelId, Model model) {
        String key = MODEL_INFO_KEY_PREFIX + modelId;
        try {
            redisTemplate.opsForValue().set(key, model, MODEL_INFO_TTL, TimeUnit.MINUTES);
            logger.debug("模型信息已缓存: {}", modelId);
        } catch (Exception e) {
            logger.error("缓存模型信息失败: {}", modelId, e);
        }
    }

    public Model getModelInfo(Long modelId) {
        String key = MODEL_INFO_KEY_PREFIX + modelId;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                logger.debug("从缓存获取模型信息: {}", modelId);
                return (Model) obj;
            }
        } catch (Exception e) {
            logger.error("获取缓存模型信息失败: {}", modelId, e);
        }
        return null;
    }

    public void evictModelInfo(Long modelId) {
        String key = MODEL_INFO_KEY_PREFIX + modelId;
        try {
            redisTemplate.delete(key);
            logger.debug("模型信息缓存已删除: {}", modelId);
        } catch (Exception e) {
            logger.error("删除模型信息缓存失败: {}", modelId, e);
        }
    }

    public void cacheUserSession(String token, String username, String role, String department) {
        String key = USER_SESSION_KEY_PREFIX + token;
        try {
            String sessionData = username + "|" + role + "|" + department;
            redisTemplate.opsForValue().set(key, sessionData, USER_SESSION_TTL, TimeUnit.HOURS);
            logger.debug("用户会话已缓存: token={}, user={}", token.substring(0, Math.min(8, token.length())), username);
        } catch (Exception e) {
            logger.error("缓存用户会话失败: token={}", token.substring(0, Math.min(8, token.length())), e);
        }
    }

    public String getUserSession(String token) {
        String key = USER_SESSION_KEY_PREFIX + token;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                logger.debug("从缓存获取用户会话: token={}", token.substring(0, Math.min(8, token.length())));
                return (String) obj;
            }
        } catch (Exception e) {
            logger.error("获取缓存用户会话失败: token={}", token.substring(0, Math.min(8, token.length())), e);
        }
        return null;
    }

    public void evictUserSession(String token) {
        String key = USER_SESSION_KEY_PREFIX + token;
        try {
            redisTemplate.delete(key);
            logger.debug("用户会话缓存已删除: token={}", token.substring(0, Math.min(8, token.length())));
        } catch (Exception e) {
            logger.error("删除用户会话缓存失败: token={}", token.substring(0, Math.min(8, token.length())), e);
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查缓存键失败: {}", key, e);
            return false;
        }
    }

    public PaginationResponse<CaseListResponse> getCaseListCache(int page, int pageSize, String caseId, String patientName) {
        String key = buildCaseListCacheKey(page, pageSize, caseId, patientName);
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                if (NULL_VALUE_MARKER.equals(obj)) {
                    logger.debug("病例列表缓存为空标记: page={}, size={}, caseId={}, patientName={}", page, pageSize, caseId, patientName);
                    return null;
                }
                logger.info("[缓存命中] 病例列表缓存: page={}, size={}, caseId={}, patientName={}", page, pageSize, caseId, patientName);
                return (PaginationResponse<CaseListResponse>) obj;
            }
        } catch (Exception e) {
            logger.error("获取病例列表缓存失败: page={}, size={}, caseId={}, patientName={}", page, pageSize, caseId, patientName, e);
        }
        return null;
    }

    public void cacheCaseList(int page, int pageSize, String caseId, String patientName, PaginationResponse<CaseListResponse> data) {
        String key = buildCaseListCacheKey(page, pageSize, caseId, patientName);
        try {
            long ttl = CASE_LIST_TTL + RANDOM.nextInt(3);
            if (data == null || data.getList().isEmpty()) {
                redisTemplate.opsForValue().set(key, NULL_VALUE_MARKER, NULL_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("病例列表空结果已缓存: page={}, size={}, caseId={}, patientName={}, TTL={}分钟", page, pageSize, caseId, patientName, NULL_CACHE_TTL);
            } else {
                redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MINUTES);
                logger.info("[缓存写入] 病例列表已缓存: page={}, size={}, caseId={}, patientName={}, TTL={}分钟, 数据量={}", page, pageSize, caseId, patientName, ttl, data.getList().size());
            }
        } catch (Exception e) {
            logger.error("缓存病例列表失败: page={}, size={}, caseId={}, patientName={}", page, pageSize, caseId, patientName, e);
        }
    }

    private String buildCaseListCacheKey(int page, int pageSize, String caseId, String patientName) {
        StringBuilder keyBuilder = new StringBuilder(CASE_LIST_KEY_PREFIX);
        keyBuilder.append("page:").append(page).append(":size:").append(pageSize);
        if (caseId != null && !caseId.isEmpty()) {
            keyBuilder.append(":caseId:").append(caseId);
        }
        if (patientName != null && !patientName.isEmpty()) {
            keyBuilder.append(":patientName:").append(patientName);
        }
        return keyBuilder.toString();
    }

    public void evictAllCaseListCache() {
        try {
            Set<String> keys = redisTemplate.keys(CASE_LIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("[缓存清理] 病例列表缓存已全部清除, 清理数量={}", keys.size());
            }
        } catch (Exception e) {
            logger.error("清理病例列表缓存失败", e);
        }
    }

    public List<ImageResponse> getImageListCache(Long caseId) {
        String key = IMAGE_LIST_KEY_PREFIX + caseId;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                if (NULL_VALUE_MARKER.equals(obj)) {
                    logger.debug("影像列表缓存为空标记: caseId={}", caseId);
                    return null;
                }
                logger.info("[缓存命中] 影像列表缓存: caseId={}", caseId);
                return (List<ImageResponse>) obj;
            }
        } catch (Exception e) {
            logger.error("获取影像列表缓存失败: caseId={}", caseId, e);
        }
        return null;
    }

    public void cacheImageList(Long caseId, List<ImageResponse> data) {
        String key = IMAGE_LIST_KEY_PREFIX + caseId;
        try {
            long ttl = IMAGE_LIST_TTL + RANDOM.nextInt(5);
            if (data == null || data.isEmpty()) {
                redisTemplate.opsForValue().set(key, NULL_VALUE_MARKER, NULL_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("影像列表空结果已缓存: caseId={}, TTL={}分钟", caseId, NULL_CACHE_TTL);
            } else {
                redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MINUTES);
                logger.info("[缓存写入] 影像列表已缓存: caseId={}, TTL={}分钟, 数据量={}", caseId, ttl, data.size());
            }
        } catch (Exception e) {
            logger.error("缓存影像列表失败: caseId={}", caseId, e);
        }
    }

    public void evictImageListCache(Long caseId) {
        String key = IMAGE_LIST_KEY_PREFIX + caseId;
        try {
            redisTemplate.delete(key);
            logger.info("[缓存清理] 影像列表缓存已清除: caseId={}", caseId);
        } catch (Exception e) {
            logger.error("清理影像列表缓存失败: caseId={}", caseId, e);
        }
    }

    public DetectionResponse getDetectionResultCache(Long detectionId) {
        String key = DETECTION_RESULT_KEY_PREFIX + detectionId;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                logger.info("[缓存命中] 检测结果缓存: detectionId={}", detectionId);
                return (DetectionResponse) obj;
            }
        } catch (Exception e) {
            logger.error("获取检测结果缓存失败: detectionId={}", detectionId, e);
        }
        return null;
    }

    public void cacheDetectionResult(Long detectionId, DetectionResponse data) {
        String key = DETECTION_RESULT_KEY_PREFIX + detectionId;
        try {
            long ttl = DETECTION_RESULT_TTL + RANDOM.nextInt(60);
            redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.HOURS);
            logger.info("[缓存写入] 检测结果已缓存: detectionId={}, TTL={}小时", detectionId, ttl);
        } catch (Exception e) {
            logger.error("缓存检测结果失败: detectionId={}", detectionId, e);
        }
    }

    public CaseResponse getCaseDetailCache(Long caseId) {
        String key = CASE_DETAIL_KEY_PREFIX + caseId;
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                if (NULL_VALUE_MARKER.equals(obj)) {
                    logger.debug("病例详情缓存为空标记: caseId={}", caseId);
                    return null;
                }
                logger.info("[缓存命中] 病例详情缓存: caseId={}", caseId);
                return (CaseResponse) obj;
            }
        } catch (Exception e) {
            logger.error("获取病例详情缓存失败: caseId={}", caseId, e);
        }
        return null;
    }

    public void cacheCaseDetail(Long caseId, CaseResponse data) {
        String key = CASE_DETAIL_KEY_PREFIX + caseId;
        try {
            long ttl = CASE_DETAIL_TTL + RANDOM.nextInt(5);
            if (data == null) {
                redisTemplate.opsForValue().set(key, NULL_VALUE_MARKER, NULL_CACHE_TTL, TimeUnit.MINUTES);
                logger.debug("病例详情空结果已缓存: caseId={}, TTL={}分钟", caseId, NULL_CACHE_TTL);
            } else {
                redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MINUTES);
                logger.info("[缓存写入] 病例详情已缓存: caseId={}, TTL={}分钟", caseId, ttl);
            }
        } catch (Exception e) {
            logger.error("缓存病例详情失败: caseId={}", caseId, e);
        }
    }

    public void evictCaseDetailCache(Long caseId) {
        String key = CASE_DETAIL_KEY_PREFIX + caseId;
        try {
            redisTemplate.delete(key);
            logger.info("[缓存清理] 病例详情缓存已清除: caseId={}", caseId);
        } catch (Exception e) {
            logger.error("清理病例详情缓存失败: caseId={}", caseId, e);
        }
    }
}
