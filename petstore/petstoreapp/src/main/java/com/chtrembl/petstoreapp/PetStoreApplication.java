package com.chtrembl.petstoreapp;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class PetStoreApplication {

    private static final String AI_CONNECTION_STRING_ENV = "APPLICATIONINSIGHTS_CONNECTION_STRING";
    private static final String APPLICATIONINSIGHTS_ENABLED = "APPLICATIONINSIGHTS_ENABLED";

    public static void main(String[] args) {
        logApplicationStart();
        configureApplicationInsights();

        SpringApplication.run(PetStoreApplication.class, args);

        log.info("Pet Store Application started successfully!");
    }

    private static void configureApplicationInsights() {
        String aiEnabledStr = System.getenv(APPLICATIONINSIGHTS_ENABLED);
        boolean aiEnabled = !"false".equalsIgnoreCase(aiEnabledStr); // Default: true

        if (!aiEnabled) {
            log.info("Application Insights DISABLED via APPLICATIONINSIGHTS_ENABLED environment variable");
            return;
        }
        
        String connectionString = System.getenv(AI_CONNECTION_STRING_ENV);

        if (StringUtils.isNotBlank(connectionString)) {
            try {
                ApplicationInsights.attach();
                log.info("Application Insights enabled successfully");
            } catch (Exception e) {
                log.warn("Failed to attach Application Insights: {}", e.getMessage());
            }
        } else {
            log.info("Application Insights not configured (no connection string found). Please set the {} environment variable with correct connection string.", AI_CONNECTION_STRING_ENV);
        }
    }

    private static void logApplicationStart() {
        log.info("Starting Pet Store Application...");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Spring Boot Version: {}", getSpringBootVersion());
        log.info("Working Directory: {}", System.getProperty("user.dir"));
    }

    private static String getSpringBootVersion() {
        try {
            Package springBootPackage = SpringApplication.class.getPackage();
            return springBootPackage.getImplementationVersion() != null
                    ? springBootPackage.getImplementationVersion()
                    : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
