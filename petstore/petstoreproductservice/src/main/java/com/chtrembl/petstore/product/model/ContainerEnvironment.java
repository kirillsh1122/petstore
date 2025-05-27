package com.chtrembl.petstore.product.model;

import ch.qos.logback.core.joran.spi.JoranException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.common.io.CharStreams;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

@Setter
@Getter
@Slf4j
public class ContainerEnvironment implements Serializable {
    private String containerHostName;
    private String appVersion;
    private String appDate;
    private String year;

	@PostConstruct
	private void initialize() throws JoranException {

		try {
			this.setContainerHostName(
					InetAddress.getLocalHost().getHostAddress() + "/" + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			this.setContainerHostName("unknown");
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
            InputStream resource = new ClassPathResource("version.json").getInputStream();
            String text = null;
            try (final Reader reader = new InputStreamReader(resource)) {
                text = CharStreams.toString(reader);
            }

            Version version = objectMapper.readValue(text, Version.class);
			this.setAppVersion(version.getVersion());
			this.setAppDate(version.getDate());
		} catch (IOException e) {
            log.info("Error parsing file {}", e.getMessage());
			this.setAppVersion("unknown");
			this.setAppDate("unknown");
		}

		this.setYear(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
	}

}
