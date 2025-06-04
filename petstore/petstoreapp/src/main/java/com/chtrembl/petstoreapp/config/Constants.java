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

    private Constants() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }
}
