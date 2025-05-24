package com.chtrembl.petstoreapp.model;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Session based for each user, each user will also have a unique Telemetry
 * Client instance.
 */
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class User implements Serializable {
	private String name = "Guest";
	private String sessionId;
	private String email;
	private List<Pet> pets;
	private List<Product> products;
	private int cartCount;
	private boolean initialTelemetryRecorded;

	@Autowired(required = false)
	private transient TelemetryClient telemetryClient;

	@Autowired
	private ContainerEnvironment containerEnvironment;

	@PostConstruct
	private void initialize() {
		if (this.telemetryClient == null) {
			this.telemetryClient = new com.chtrembl.petstoreapp.service.TelemetryClient();
		}
	}

	public synchronized void setPets(List<Pet> pets) {
		this.pets = pets;
	}

	public synchronized void setProducts(List<Product> products) {
		this.products = products;
	}

	public Map<String, String> getCustomEventProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("session_Id", this.sessionId);
		properties.put("appDate", this.containerEnvironment.getAppDate());
		properties.put("appVersion", this.containerEnvironment.getAppVersion());
		properties.put("containerHostName", this.containerEnvironment.getContainerHostName());
		return properties;
	}
}
