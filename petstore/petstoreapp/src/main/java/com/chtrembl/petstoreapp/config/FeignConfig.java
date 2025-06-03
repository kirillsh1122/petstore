package com.chtrembl.petstoreapp.config;

import com.chtrembl.petstoreapp.model.User;
import com.chtrembl.petstoreapp.model.WebRequest;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignConfig {

    private final WebRequest webRequest;
    private final User sessionUser;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new EnhancedRequestInterceptor();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new PetstoreErrorDecoder();
    }

    @Bean
    public feign.Request.Options feignOptions() {
        return new feign.Request.Options(
                5000, TimeUnit.MILLISECONDS,  // connect timeout
                5000, TimeUnit.MILLISECONDS,  // read timeout
                true  // follow redirects
        );
    }

    private static class PetstoreErrorDecoder implements ErrorDecoder {
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            String requestId = extractHeaderValue(response, "X-Request-ID");
            String sessionId = extractHeaderValue(response, "X-Session-ID");

            log.error("Feign client error on {} [RequestID: {}, SessionID: {}]: {} - {}",
                    methodKey, requestId, sessionId, response.status(), response.reason());

            String errorMessage = String.format("Service call failed for %s [RequestID: %s, SessionID: %s] with status %d",
                    methodKey, requestId, sessionId, response.status());

            return switch (response.status()) {
                case 404 -> new feign.FeignException.NotFound(
                        "Resource not found for " + methodKey,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
                case 400 -> new feign.FeignException.BadRequest(
                        "Bad request for " + methodKey,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
                case 429 -> new feign.FeignException.TooManyRequests(
                        "Rate limit exceeded for " + methodKey,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
                case 500 -> new feign.FeignException.InternalServerError(
                        "Internal server error for " + methodKey,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
                case 503 -> new feign.FeignException.ServiceUnavailable(
                        "Service unavailable for " + methodKey,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
                default -> new feign.FeignException.BadRequest(
                        errorMessage,
                        response.request(),
                        extractResponseBody(response),
                        response.headers()
                );
            };
        }

        private byte[] extractResponseBody(feign.Response response) {
            try {
                if (response.body() != null) {
                    return response.body().toString().getBytes();
                }
            } catch (Exception e) {
                log.warn("Failed to extract response body: {}", e.getMessage());
            }
            return new byte[0];
        }

        private String extractHeaderValue(feign.Response response, String headerName) {
            try {
                return response.headers().getOrDefault(headerName, java.util.Collections.emptyList())
                        .stream()
                        .findFirst()
                        .orElse("unknown");
            } catch (Exception e) {
                return "unknown";
            }
        }
    }

    private class EnhancedRequestInterceptor implements RequestInterceptor {

        @Override
        public void apply(RequestTemplate template) {
            webRequest.getHeaders().forEach((key, values) -> {
                values.forEach(value -> template.header(key, value));
            });

            template.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            template.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            template.header("Cache-Control", "no-cache");

            addSessionHeaders(template);
            addCorrelationHeaders(template);
            addUserContextHeaders(template);
            addServiceHeaders(template);

            template.header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));

            log.debug("Applied headers to Feign request: {} {}",
                    template.method(), template.url());
            log.debug("All headers: {}", template.headers());
        }

        private void addSessionHeaders(RequestTemplate template) {
            if (sessionUser != null && StringUtils.hasText(sessionUser.getSessionId())) {
                template.header("X-Session-ID", sessionUser.getSessionId());
                template.header("X-Session-Id", sessionUser.getSessionId());
                log.debug("Added session ID header: {}", sessionUser.getSessionId());
            }

            try {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                String sessionId = attributes.getRequest().getSession().getId();
                if (StringUtils.hasText(sessionId)) {
                    template.header("X-Http-Session-ID", sessionId);
                }
            } catch (Exception e) {
                log.debug("Could not extract HTTP session ID: {}", e.getMessage());
            }
        }

        private void addCorrelationHeaders(RequestTemplate template) {
            String requestId = MDC.get("requestId");
            if (StringUtils.hasText(requestId)) {
                template.header("X-Request-ID", requestId);
                template.header("X-Correlation-ID", requestId);
            } else {
                String newRequestId = UUID.randomUUID().toString().substring(0, 8);
                template.header("X-Request-ID", newRequestId);
                template.header("X-Correlation-ID", newRequestId);
                log.debug("Generated new request ID: {}", newRequestId);
            }

            String traceId = MDC.get("traceId");
            if (StringUtils.hasText(traceId)) {
                template.header("X-Trace-ID", traceId);
            }

            String spanId = MDC.get("spanId");
            if (StringUtils.hasText(spanId)) {
                template.header("X-Parent-Span-ID", spanId);
            }
        }

        private void addUserContextHeaders(RequestTemplate template) {
            if (sessionUser != null) {
                if (StringUtils.hasText(sessionUser.getName())) {
                    template.header("X-User-Name", sessionUser.getName());
                }

                if (StringUtils.hasText(sessionUser.getEmail())) {
                    template.header("X-User-Email", sessionUser.getEmail());
                }

                String authType = MDC.get("authType");
                if (StringUtils.hasText(authType)) {
                    template.header("X-Auth-Type", authType);
                }

                String isAuthenticated = MDC.get("isAuthenticated");
                if (StringUtils.hasText(isAuthenticated)) {
                    template.header("X-Authenticated", isAuthenticated);
                }
            }
        }

        private void addServiceHeaders(RequestTemplate template) {
            template.header("X-Source-Service", "petstoreapp");
            template.header("X-Source-Version", getAppVersion());

            String targetService = template.feignTarget().name();
            template.header("X-Target-Service", targetService);

            template.header("X-Request-URI", MDC.get("requestURI"));
            template.header("X-Request-Method", MDC.get("requestMethod"));

            String containerHost = MDC.get("containerHost");
            if (StringUtils.hasText(containerHost)) {
                template.header("X-Source-Container", containerHost);
            }
        }

        private String getAppVersion() {
            String version = System.getProperty("app.version");
            if (StringUtils.hasText(version)) {
                return version;
            }

            Package pkg = this.getClass().getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                return pkg.getImplementationVersion();
            }

            return "unknown";
        }
    }
}