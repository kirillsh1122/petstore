package com.chtrembl.petstoreapp.controller;

import com.chtrembl.petstoreapp.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest API controller for the PetStore application.
 * Provides endpoints for contact information and session introspection.
 */

@RestController
public class RestAPIController {

	@Autowired
	private User sessionUser;

	@GetMapping("/api/contactus")
	public String contactus() {
		this.sessionUser.getTelemetryClient().trackEvent(
				String.format("PetStoreApp user %s requesting Contact Us", this.sessionUser.getName()),
				this.sessionUser.getCustomEventProperties(), null);
		return "Please contact Azure PetStore at 401-555-5555. Thank you. Demo 6/13";
	}

	@GetMapping("/api/sessionid")
	public String sessionid() {
		return this.sessionUser.getSessionId();
	}
}

