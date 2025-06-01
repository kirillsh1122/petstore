package com.chtrembl.petstoreapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class MDCInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        MDC.put("requestURI", request.getRequestURI());
        MDC.put("requestMethod", request.getMethod());

        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        log.debug("Starting request processing");

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        try {
            MDC.put("responseStatus", String.valueOf(response.getStatus()));

            if (ex != null) {
                MDC.put("hasException", "true");
                log.error("Request completed with exception", ex);
            } else {
                log.debug("Request completed successfully");
            }
        } finally {
            MDC.clear();
        }
    }
}