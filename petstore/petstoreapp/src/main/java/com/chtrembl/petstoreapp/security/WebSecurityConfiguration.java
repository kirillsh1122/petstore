package com.chtrembl.petstoreapp.security;

import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfiguration.class);

	private final AADB2COidcLoginConfigurerWrapper aadB2COidcLoginConfigurerWrapper;
	private final ContainerEnvironment containeEnvironment;

	@Value("${petstore.security.enabled:true}")
	private boolean securityEnabled;

	@Override
	public void configure(WebSecurity web) throws Exception {
		if (aadB2COidcLoginConfigurerWrapper != null &&
				aadB2COidcLoginConfigurerWrapper.getConfigurer() != null) {
			web.ignoring().antMatchers("/content/**");
			web.ignoring().antMatchers("/.well-known/**");
		}
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		if (!securityEnabled) {
			http.csrf().disable()
					.authorizeRequests().anyRequest().permitAll();
			containeEnvironment.setSecurityEnabled(false);
			logger.warn("Security is DISABLED via petstore.security.enabled = false");
			return;
		}

		if (aadB2COidcLoginConfigurerWrapper != null &&
				aadB2COidcLoginConfigurerWrapper.getConfigurer() != null) {

			http.csrf()
					.and()
					.authorizeRequests().antMatchers("/").permitAll()
					.antMatchers("/*breed*").permitAll()
					.antMatchers("/*product*").permitAll()
					.antMatchers("/*cart*").permitAll()
					.antMatchers("/api/contactus").permitAll()
					.antMatchers("/login*").permitAll()
					.anyRequest().authenticated()
					.and()
					.apply(aadB2COidcLoginConfigurerWrapper.getConfigurer())
					.and()
					.oauth2Login().loginPage("/login");

			containeEnvironment.setSecurityEnabled(true);
			logger.info("Security is ENABLED using Azure B2C configuration.");
		} else {
			http.csrf().disable()
					.authorizeRequests().anyRequest().permitAll();
			containeEnvironment.setSecurityEnabled(false);
			logger.warn("Security ENABLED in config but Azure B2C not configured — fallback to DISABLED");
		}
	}
}
