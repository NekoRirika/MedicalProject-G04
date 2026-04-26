package com.xycy.chestimaging.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class PerformanceInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;

    @Autowired
    private MeterRegistry meterRegistry;

    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTimeThreadLocal.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Long startTime = startTimeThreadLocal.get();
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();

            Timer.builder("http.request.duration")
                    .description("HTTP请求耗时")
                    .tag("uri", uri)
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                logger.warn("[性能监控] 慢请求: {} {} - 耗时: {}ms, 状态码: {}", method, uri, duration, status);
            } else {
                logger.debug("[性能监控] 请求: {} {} - 耗时: {}ms, 状态码: {}", method, uri, duration, status);
            }
        }
        startTimeThreadLocal.remove();
    }
}
