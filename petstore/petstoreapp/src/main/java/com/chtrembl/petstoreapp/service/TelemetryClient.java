package com.chtrembl.petstoreapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * Custom TelemetryClient that works with the modern Application Insights Java agent.
 * In Spring Boot 3 with Application Insights 3.7+, telemetry is automatically collected
 * through the Java agent, so this class primarily provides logging integration.
 */
@Component
public class TelemetryClient {
	private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);
    private static final Logger telemetryLogger = LoggerFactory.getLogger("ApplicationInsightsTelemetry");

    public void track(Object telemetry) {
        telemetryLogger.info("Custom telemetry tracked: {}", telemetry);
        logger.debug("Telemetry object processed: {}", telemetry.getClass().getSimpleName());
	}

    public void trackDependency(String dependencyName, String commandName, Object duration, boolean success) {
        telemetryLogger.info("Dependency: {} - {} (Success: {})", dependencyName, commandName, success);
        logger.debug("Dependency call tracked: {} -> {}", dependencyName, commandName);
	}

	public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        // Add properties to MDC for structured logging
        if (properties != null) {
            properties.forEach(MDC::put);
        }

        telemetryLogger.info("Event: {} with properties: {} and metrics: {}", name, properties, metrics);
        logger.info("Custom event tracked: {}", name);

        // Clean up MDC
        if (properties != null) {
            properties.keySet().forEach(MDC::remove);
        }
	}

	public void trackEvent(String name) {
        telemetryLogger.info("Event: {}", name);
        logger.info("Simple event tracked: {}", name);
	}

	public void trackException(Exception exception, Map<String, String> properties, Map<String, Double> metrics) {
        if (properties != null) {
            properties.forEach(MDC::put);
        }

        telemetryLogger.error("Exception tracked with properties: {} and metrics: {}", properties, metrics, exception);
        logger.error("Exception tracked: {}", exception.getMessage(), exception);

        if (properties != null) {
            properties.keySet().forEach(MDC::remove);
        }
	}

	public void trackException(Exception exception) {
        telemetryLogger.error("Exception tracked", exception);
        logger.error("Simple exception tracked: {}", exception.getMessage(), exception);
	}

	public void trackHttpRequest(String name, Date timestamp, long duration, String responseCode, boolean success) {
        telemetryLogger.info("HTTP Request: {} - {} ms (Response: {}, Success: {})", name, duration, responseCode, success);
        logger.debug("HTTP request logged: {} took {} ms", name, duration);
	}

    public void trackMetric(String name, double value, int sampleCount, double min, double max, Map<String, String> properties) {
        if (properties != null) {
            properties.forEach(MDC::put);
        }

        telemetryLogger.info("Metric: {} = {} (samples: {}, min: {}, max: {}) with properties: {}",
                name, value, sampleCount, min, max, properties);
        logger.debug("Metric tracked: {} = {}", name, value);

        if (properties != null) {
            properties.keySet().forEach(MDC::remove);
        }
	}

	public void trackMetric(String name, double value) {
        telemetryLogger.info("Metric: {} = {}", name, value);
        logger.debug("Simple metric tracked: {} = {}", name, value);
	}

    public void trackPageView(Object pageViewTelemetry) {
        telemetryLogger.info("Page view tracked: {}", pageViewTelemetry);
        logger.debug("Page view processed: {}", pageViewTelemetry);
	}

	public void trackPageView(String name) {
        telemetryLogger.info("Page view: {}", name);
        logger.info("Page view tracked: {}", name);
	}

    public void trackTrace(String message, Object severityLevel, Map<String, String> properties) {
        if (properties != null) {
            properties.forEach(MDC::put);
        }

        telemetryLogger.info("Trace [{}]: {} with properties: {}", severityLevel, message, properties);
        logger.debug("Trace logged with level: {}", severityLevel);

        if (properties != null) {
            properties.keySet().forEach(MDC::remove);
        }
	}

    public void trackTrace(String message, Object severityLevel) {
        telemetryLogger.info("Trace [{}]: {}", severityLevel, message);
        logger.debug("Simple trace: {}", message);
	}

	public void trackTrace(String message) {
        telemetryLogger.info("Trace: {}", message);
        logger.info("Trace message: {}", message);
	}

    public void flush() {
        telemetryLogger.info("Telemetry flush requested");
        logger.debug("Telemetry flush operation called");
	}
}
