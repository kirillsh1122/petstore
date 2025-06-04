package com.chtrembl.petstoreapp.controller;

import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Order;
import com.chtrembl.petstoreapp.model.Pet;
import com.chtrembl.petstoreapp.model.User;
import com.chtrembl.petstoreapp.service.PetStoreFacadeService;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

/**
 * Controller for the PetStore web application.
 * Handles requests for various pages and manages user sessions.
 */

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebAppController {
	private static final String CURRENT_USERS_HUB = "currentUsers";

	private final ContainerEnvironment containerEnvironment;
	private final PetStoreFacadeService petStoreService;
	private final User sessionUser;
	private final CacheManager currentUsersCacheManager;

	@ModelAttribute
	public void setModel(HttpServletRequest request, Model model, OAuth2AuthenticationToken token) {

		CaffeineCache caffeineCache = (CaffeineCache) this.currentUsersCacheManager
				.getCache(CURRENT_USERS_HUB);
		com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

		if (this.sessionUser.getSessionId() == null) {
			String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
			this.sessionUser.setSessionId(sessionId);
			caffeineCache.put(this.sessionUser.getSessionId(), this.sessionUser.getName());
		}

		MDC.put("sessionId", this.sessionUser.getSessionId());
		MDC.put("userName", this.sessionUser.getName());
		MDC.put("containerHost", this.containerEnvironment.getContainerHostName());

		caffeineCache.put(this.sessionUser.getSessionId(), this.sessionUser.getName());

		if (token != null) {
			final OAuth2User user = token.getPrincipal();

			String userEmail = extractUserEmail(user);
			if (userEmail != null) {
				this.sessionUser.setEmail(userEmail);
				MDC.put("userEmail", userEmail);
				log.debug("User email set to: {}", userEmail);
			} else {
				log.warn("Could not extract email for user: {}", this.sessionUser.getName());
			}

			this.sessionUser.setName((String) user.getAttributes().get("name"));
			MDC.put("userName", this.sessionUser.getName());
			MDC.put("authType", "OAuth2");
			MDC.put("isAuthenticated", "true");

			if (!this.sessionUser.isInitialTelemetryRecorded()) {
				this.sessionUser.getTelemetryClient().trackEvent(
						String.format("PetStoreApp %s logged in, container host: %s", this.sessionUser.getName(),
								this.containerEnvironment.getContainerHostName()),
						this.sessionUser.getCustomEventProperties(), null);

				this.sessionUser.setInitialTelemetryRecorded(true);
			}
			model.addAttribute("claims", user.getAttributes());
			model.addAttribute("user", this.sessionUser.getName());
			model.addAttribute("grant_type", user.getAuthorities());
		} else {
			MDC.put("authType", "Anonymous");
			MDC.put("isAuthenticated", "false");
		}

		model.addAttribute("userName", this.sessionUser.getName());
		model.addAttribute("containerEnvironment", this.containerEnvironment);
		model.addAttribute("sessionId", this.sessionUser.getSessionId());
		model.addAttribute("appVersion", this.containerEnvironment.getAppVersion());
		model.addAttribute("cartSize", this.sessionUser.getCartCount());
		model.addAttribute("currentUsersOnSite", nativeCache.asMap().size());
	}

	@GetMapping(value = "/login")
	public String login(Model model, HttpServletRequest request) throws URISyntaxException {
		log.debug("PetStoreApp /login requested, routing to login view...");

		PageViewTelemetry pageViewTelemetry = new PageViewTelemetry();
		pageViewTelemetry.setUrl(new URI(request.getRequestURL().toString()));
		pageViewTelemetry.setName("login");
		this.sessionUser.getTelemetryClient().trackPageView(pageViewTelemetry);
		return "login";
	}

	@GetMapping(value = { "/dogbreeds", "/catbreeds", "/fishbreeds" })
	public String breeds(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
						 @RequestParam(name = "category") String category) throws URISyntaxException {
		if (!"Dog".equals(category) && !"Cat".equals(category) && !"Fish".equals(category)) {
			return "home";
		}
		try {
			final Collection<Pet> pets = this.petStoreService.getPets(category);
			model.addAttribute("pets", pets);
		} catch (Exception ex) {
			log.error("Error loading pets from service: ", ex);
			model.addAttribute("error", "Sorry, we couldn't load pet breeds.");
			model.addAttribute("stacktrace", getStackTrace(ex));
		}
		return "breeds";
	}

	@GetMapping(value = "/breeddetails")
	public String breedeetails(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
							   @RequestParam(name = "category") String category,
							   @RequestParam(name = "id") int id) throws URISyntaxException {

		if (!"Dog".equals(category) && !"Cat".equals(category) && !"Fish".equals(category)) {
			return "home";
		}

		try {
			if (this.sessionUser.getPets() == null) {
				this.petStoreService.getPets(category);
			}

			Pet pet = this.sessionUser.getPets().get(id - 1);
			log.debug("PetStoreApp /breeddetails requested for {}, routing to dogbreeddetails view...", pet.getName());
			model.addAttribute("pet", pet);

		} catch (Exception ex) {
			log.error("Error loading pet details: ", ex);
			model.addAttribute("error", "Sorry, we couldn't load pet details.");
			model.addAttribute("stacktrace", getStackTrace(ex));
		}

		return "breeddetails";
	}

	@GetMapping(value = "/products")
	public String products(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
						   @RequestParam(name = "category") String category,
						   @RequestParam(name = "id") int id) throws URISyntaxException {

		if (!"Toy".equals(category) && !"Food".equals(category)) {
			return "home";
		}

		try {
			log.debug("PetStoreApp /products requested for {}, routing to products view...", category);
			Pet pet = this.sessionUser.getPets().get(id - 1);
			model.addAttribute("products",
					this.petStoreService.getProducts(pet.getCategory().getName() + " " + category, pet.getTags()));
		} catch (Exception ex) {
			log.error("Error loading products: ", ex);
			model.addAttribute("error", "Sorry, we couldn't load products.");
			model.addAttribute("stacktrace", getStackTrace(ex));
		}

		return "products";
	}

	@GetMapping(value = "/cart")
	public String cart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
		try {
			Order order = this.petStoreService.retrieveOrder(this.sessionUser.getSessionId());
			model.addAttribute("order", order);

			int cartSize = 0;
			if (order != null && order.getProducts() != null && !order.isComplete()) {
				cartSize = order.getProducts().size();
			}
			this.sessionUser.setCartCount(cartSize);
			model.addAttribute("cartSize", this.sessionUser.getCartCount());

			if (token != null) {
				model.addAttribute("userLoggedIn", true);
				model.addAttribute("email", this.sessionUser.getEmail());
			}

		} catch (Exception ex) {
			log.error("Error loading cart: ", ex);
			model.addAttribute("error", "Sorry, we couldn't load your cart.");
			model.addAttribute("stacktrace", getStackTrace(ex));
		}

		return "cart";
	}

	@PostMapping(value = "/updatecart")
	public String updateCart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
							 @RequestParam Map<String, String> params) {
		int cartCount = 1;

		String operator = params.get("operator");
		if (StringUtils.isNotEmpty(operator)) {
			if ("minus".equals(operator)) {
				cartCount = -1;
			} else if ("remove".equals(operator)) {
				cartCount = -999;
			}
		}

		this.petStoreService.updateOrder(Long.parseLong(params.get("productId")), cartCount, false);
		return "redirect:cart";
	}

	@PostMapping(value = "/completecart")
	public String updateCart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
		if (token != null) {
			this.petStoreService.updateOrder(0, 0, true);
		}
		return "redirect:cart";
	}

	@GetMapping(value = "/claims")
	public String claims(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
			throws URISyntaxException {
		log.debug("PetStoreApp /claims requested for {}, routing to claims view...", this.sessionUser.getName());
		return "claims";
	}

	@GetMapping("/fail")
	public String fail() {
		throw new RuntimeException("Test 500 error");
	}

	@GetMapping(value = {"/", "/home.htm*", "/index.htm*"})
	public String landing(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
			throws URISyntaxException {
		return "home";
	}

	private String getStackTrace(Throwable throwable) {
		StringBuilder sb = new StringBuilder();
		appendStackTrace(sb, throwable, "");
		return sb.toString();
	}

	private void appendStackTrace(StringBuilder sb, Throwable throwable, String prefix) {
		if (throwable == null) return;

		sb.append(prefix).append(throwable.toString()).append("\n");
		for (StackTraceElement element : throwable.getStackTrace()) {
			sb.append("\tat ").append(element).append("\n");
		}

		for (Throwable suppressed : throwable.getSuppressed()) {
			appendStackTrace(sb, suppressed, "Suppressed: ");
		}

		Throwable cause = throwable.getCause();
		if (cause != null && cause != throwable) {
			appendStackTrace(sb, cause, "Caused by: ");
		}
	}

	private String extractUserEmail(OAuth2User user) {
		try {
			Object emailsAttribute = user.getAttribute("emails");
			if (emailsAttribute instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<String> emails = (Collection<String>) emailsAttribute;
				if (!emails.isEmpty()) {
					String email = emails.iterator().next();
					log.debug("Found email from 'emails' collection: {}", email);
					return email.trim();
				}
			}
			log.warn("No email or subject ID found in OAuth2 attributes");
		} catch (Exception e) {
			log.warn("Error extracting email from OAuth2User: {}", e.getMessage(), e);
		}
		return null;
	}
}
