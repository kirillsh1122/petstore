package com.chtrembl.petstoreapp.config;

public final class Constants {

    // MDC Keys
    public static final String CLIENT_IP = "clientIp";
    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String EXCEPTION_TYPE = "exceptionType";
    public static final String HAS_EXCEPTION = "hasException";
    public static final String PARENT_SPAN_ID = "parentSpanId";
    public static final String REFERER = "referer";
    public static final String REQUEST_DURATION = "requestDuration";
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URI = "requestURI";
    public static final String RESPONSE_STATUS = "responseStatus";
    public static final String SESSION_ID = "sessionId";
    public static final String SPAN_ID = "spanId";
    public static final String TRACE_ID = "traceId";
    public static final String USER_AGENT = "userAgent";

    // WebAppController constants
    public static final String AUTH_TYPE = "authType";
    public static final String CONTAINER_HOST = "containerHost";
    public static final String IS_AUTHENTICATED = "isAuthenticated";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_NAME = "userName";

    // Service operation constants
    public static final String CATEGORY = "category";
    public static final String COMPLETE_ORDER = "completeOrder";
    public static final String OPERATION = "operation";
    public static final String ORDER_ID = "orderId";
    public static final String PRODUCT_ID = "productId";
    public static final String QUANTITY = "quantity";

    // HTTP Headers
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_PARENT_SPAN_ID = "X-Parent-Span-ID";
    public static final String X_REQUEST_DURATION = "X-Request-Duration";
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_SPAN_ID = "X-Span-ID";
    public static final String X_TRACE_ID = "X-Trace-ID";
    public static final String X_RESPONSE_TRACE_ID = "X-Response-Trace-ID";
    public static final String X_RESPONSE_SPAN_ID = "X-Response-Span-ID";
    public static final String X_RESPONSE_REQUEST_ID = "X-Response-Request-ID";

    // Client IP Headers
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

    // Other Headers
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String REFERER_HEADER = "Referer";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String X_REQUEST_TIMESTAMP = "X-Request-Timestamp";
    public static final String X_SESSION_ID = "X-Session-ID";
    public static final String X_SESSION_ID_LOWERCASE = "X-Session-Id";
    public static final String X_HTTP_SESSION_ID = "X-Http-Session-ID";
    public static final String X_USER_NAME = "X-User-Name";
    public static final String X_USER_EMAIL = "X-User-Email";
    public static final String X_AUTH_TYPE = "X-Auth-Type";
    public static final String X_AUTHENTICATED = "X-Authenticated";
    public static final String X_SOURCE_SERVICE = "X-Source-Service";
    public static final String X_SOURCE_VERSION = "X-Source-Version";
    public static final String X_TARGET_SERVICE = "X-Target-Service";
    public static final String X_REQUEST_URI = "X-Request-URI";
    public static final String X_REQUEST_METHOD = "X-Request-Method";
    public static final String X_SOURCE_CONTAINER = "X-Source-Container";

    private Constants() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }
}
