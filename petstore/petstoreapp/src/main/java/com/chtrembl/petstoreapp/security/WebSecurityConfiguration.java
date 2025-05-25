package com.chtrembl.petstoreapp.security;

import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    private final ContainerEnvironment containerEnvironment;

	@Value("${petstore.security.enabled:true}")
	private boolean securityEnabled;

    // Прямая проверка Azure B2C properties
    @Value("${spring.cloud.azure.active-directory.b2c.base-uri:}")
    private String azureB2cBaseUri;

    @Value("${spring.cloud.azure.active-directory.b2c.credential.client-id:}")
    private String azureB2cClientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		if (!securityEnabled) {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            containerEnvironment.setSecurityEnabled(false);
			logger.warn("Security is DISABLED via petstore.security.enabled = false");
            return http.build();
		}

        // Проверяем Azure B2C конфигурацию напрямую
        boolean azureB2cConfigured = StringUtils.hasText(azureB2cBaseUri) &&
                StringUtils.hasText(azureB2cClientId);

        if (azureB2cConfigured) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/content/**", "/.well-known/**"))
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/").permitAll()
                            .requestMatchers("/*breed*").permitAll()
                            .requestMatchers("/*product*").permitAll()
                            .requestMatchers("/*cart*").permitAll()
                            .requestMatchers("/api/contactus").permitAll()
                            .requestMatchers("/login*").permitAll()
                            .requestMatchers("/content/**").permitAll()
                            .requestMatchers("/.well-known/**").permitAll()
                            .anyRequest().authenticated())
                    .oauth2Login(oauth2 -> oauth2
                            .loginPage("/login")
                            .defaultSuccessUrl("/", true));

            containerEnvironment.setSecurityEnabled(true);
            logger.info("Security is ENABLED using Azure B2C auto-configuration.");
		} else {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            containerEnvironment.setSecurityEnabled(false);
			logger.warn("Security ENABLED in config but Azure B2C not configured — fallback to DISABLED");
		}

        return http.build();
	}
}