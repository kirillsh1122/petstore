package com.chtrembl.petstoreapp.model;

import ch.qos.logback.core.joran.spi.JoranException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Singleton to store container state
 */
@SuppressWarnings("serial")
@Component
@EnableScheduling
public class ContainerEnvironment implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(ContainerEnvironment.class);
	private String containerHostName = null;
	private String appVersion = null;
	private String appDate = null;
	private String year = null;

	private boolean securityEnabled = false;

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

	@Value("${ga.tracking.id:}")
	private String gaTrackingId;

	@Value("${bing.search.url:https://api.bing.microsoft.com/}")
	private String bingSearchURL;

	@Value("${bing.search.subscription.key:}")
	private String bingSearchSubscriptionKey;

	@Value("#{T(java.util.Arrays).asList('${petstore.logging.additional-headers-to-log:}')}") 
	private List<String> additionalHeadersToLog;

	@Value("#{T(java.util.Arrays).asList('${petstore.logging.additional-headers-to-send:}')}") 
	private List<String> additionalHeadersToSend;


	@Autowired
	private CacheManager currentUsersCacheManager;

	@PostConstruct
	private void initialize() throws JoranException {
		// LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			this.setContainerHostName(
					InetAddress.getLocalHost().getHostAddress() + "/" + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			this.setContainerHostName("unknown");
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Version version = objectMapper.readValue(new ClassPathResource("version.json").getFile(), Version.class);
			this.setAppVersion(version.getVersion());
			this.setAppDate(version.getDate());
		} catch (IOException e) {
			logger.info("error parsing file " + e.getMessage());
			this.setAppVersion("unknown");
			this.setAppDate("unknown");
		}

		this.setYear(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

		// fix this at some point so it doesnt need to be done in the filter each
		// time...
		// context.putProperty("appVersion", this.getAppVersion());
		// context.putProperty("appDate", this.getAppDate());
		// context.putProperty("containerHostName", this.getContainerHostName());
	}


	public String getContainerHostName() {
		return containerHostName;
	}

	public void setContainerHostName(String containerHostName) {
		this.containerHostName = containerHostName;
	}

	public String getAppVersion() {
		if ("version".equals(this.appVersion) || this.appVersion == null) {
			return String.valueOf(System.currentTimeMillis());
		}
		return this.appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getPetStoreServicesSubscriptionKey() {
		return petStoreServicesSubscriptionKey;
	}

	public void setPetStoreServicesSubscriptionKey(String petStoreServicesSubscriptionKey) {
		this.petStoreServicesSubscriptionKey = petStoreServicesSubscriptionKey;
	}

	public String getAppDate() {
		return appDate;
	}

	public void setAppDate(String appDate) {
		this.appDate = appDate;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public boolean isSecurityEnabled() {
		return securityEnabled;
	}

	public void setSecurityEnabled(boolean securityEnabled) {
		this.securityEnabled = securityEnabled;
	}

	public String getPetStorePetServiceURL() {
		return petStorePetServiceURL;
	}

	public String getPetStoreProductServiceURL() {
		return petStoreProductServiceURL;
	}

	public String getPetStoreOrderServiceURL() {
		return petStoreOrderServiceURL;
	}

	public String getPetstoreAPIMHost() {
		return petstoreAPIMHost;
	}

	public String getGaTrackingId() {
		return gaTrackingId;
	}

	public String getBingSearchURL() {
		return bingSearchURL;
	}

	public String getBingSearchSubscriptionKey() {
		return bingSearchSubscriptionKey;
	}

	public List<String> getAdditionalHeadersToLog() {
		return additionalHeadersToLog;
	}

	public List<String> getAdditionalHeadersToSend() {
		return additionalHeadersToSend;
	}

}
