package com.chtrembl.petstoreapp.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

/**
 * Singleton to store container state
 */
@Component
@EnableScheduling
@Getter
@Setter
public class ContainerEnvironment implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(ContainerEnvironment.class);
	private String containerHostName;
	private String appVersion;
	private String appDate;
	private String year;
	private boolean securityEnabled;

	@Value("${petstore.service.pet.url:}")
	private String petStorePetServiceURL;

	@Value("${petstore.service.product.url:}")
	private String petStoreProductServiceURL;

	@Value("${petstore.service.order.url:}")
	private String petStoreOrderServiceURL;

	@Value("${petstore.service.subscription.key:}")
	private String petStoreServicesSubscriptionKey;

	@Value("${petstore.apim.host:}")
	private String petstoreAPIMHost;

	@Autowired
	private CacheManager currentUsersCacheManager;

	@PostConstruct
	private void initialize() {
		try {
			this.setContainerHostName(
					InetAddress.getLocalHost().getHostAddress() + "/" + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			this.setContainerHostName("unknown");
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			ClassPathResource resource = new ClassPathResource("version.json");

			try (InputStream inputStream = resource.getInputStream()) {
				Version version = objectMapper.readValue(inputStream, Version.class);
				this.setAppVersion(version.getVersion());
				this.setAppDate(version.getDate());
			}
		} catch (IOException e) {
			logger.info("Error parsing file " + e.getMessage());
			this.setAppVersion("unknown");
			this.setAppDate("unknown");
		}

		this.setYear(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
	}

	public String getAppVersion() {
		if ("version".equals(this.appVersion) || this.appVersion == null) {
			return String.valueOf(System.currentTimeMillis());
		}
		return this.appVersion;
	}
}
