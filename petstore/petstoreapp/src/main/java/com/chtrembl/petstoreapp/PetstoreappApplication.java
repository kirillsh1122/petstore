package com.chtrembl.petstoreapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.microsoft.applicationinsights.attach.ApplicationInsights;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;

import java.net.InetAddress;
import java.net.UnknownHostException;


@SpringBootApplication
public class PetstoreappApplication {
	
	private static final String AI_CONNECTION_STRING_ENV = "APPLICATIONINSIGHTS_CONNECTION_STRING";
    private static final String APPLICATIONINSIGHTS_ENABLED = "APPLICATIONINSIGHTS_ENABLED";
	
	private static Logger logger = LoggerFactory.getLogger(PetstoreappApplication.class);

	public static void main(String[] args) {
		configureApplicationInsights();
		SpringApplication.run(PetstoreappApplication.class, args);
		logger.info("PetStoreApp started up... " + System.getProperty("catalina.base"));
		try {
			InetAddress address = InetAddress.getByName("www.google.com");
			logger.info("GOOGLE: " + address.getHostAddress());
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void configureApplicationInsights() {
        String aiEnabledStr = System.getenv(APPLICATIONINSIGHTS_ENABLED);
        boolean aiEnabled = !"false".equalsIgnoreCase(aiEnabledStr); // Default: true

        if (!aiEnabled) {
        	logger.info("Application Insights DISABLED via APPLICATIONINSIGHTS_ENABLED environment variable");
            return;
        }

        String connectionString = System.getenv(AI_CONNECTION_STRING_ENV);

        if (StringUtils.isNotBlank(connectionString)) {
            try {
                ApplicationInsights.attach();
                ConnectionString.configure(connectionString);
                logger.info("Application Insights enabled successfully");
            } catch (Exception e) {
            	logger.warn("Failed to attach Application Insights: {}", e.getMessage());
            }
        } else {
        	logger.info("Application Insights not configured (no connection string found). Please set the {} environment variable with correct connection string.", AI_CONNECTION_STRING_ENV);
        }
    }
}
