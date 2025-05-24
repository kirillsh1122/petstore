package com.chtrembl.petstoreapp.controller;

import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Order;
import com.chtrembl.petstoreapp.model.Pet;
import com.chtrembl.petstoreapp.model.User;
import com.chtrembl.petstoreapp.service.PetStoreService;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.nimbusds.jose.shaded.json.JSONArray;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebAppController {
	private static final Logger logger = LoggerFactory.getLogger(WebAppController.class);
	private static final String CURRENT_USERS_HUB = "currentUsers";

	private final ContainerEnvironment containerEnvironment;
	private final PetStoreService petStoreService;
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

		caffeineCache.put(this.sessionUser.getSessionId(), this.sessionUser.getName());

		if (token != null) {
			final OAuth2User user = token.getPrincipal();

			try {
				this.sessionUser.setEmail((String) ((JSONArray) user.getAttribute("emails")).get(0));
			} catch (Exception e) {
				logger.warn(String.format("PetStoreApp  %s logged in, however cannot get email associated: %s",
						this.sessionUser.getName(), e.getMessage()));
			}

			this.sessionUser.setName((String) user.getAttributes().get("name"));

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
		}

		model.addAttribute("userName", this.sessionUser.getName());
		model.addAttribute("containerEnvironment", this.containerEnvironment);
		model.addAttribute("sessionId", this.sessionUser.getSessionId());
		model.addAttribute("appVersion", this.containerEnvironment.getAppVersion());
		model.addAttribute("cartSize", this.sessionUser.getCartCount());
		model.addAttribute("currentUsersOnSite", nativeCache.asMap().size());

		MDC.put("session_Id", this.sessionUser.getSessionId());
	}

	@GetMapping(value = "/login")
	public String login(Model model, HttpServletRequest request) throws URISyntaxException {
		logger.info("PetStoreApp /login requested, routing to login view...");

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
		logger.info(String.format("PetStoreApp /breeds requested for %s, routing to breeds view...", category));

		model.addAttribute("pets", this.petStoreService.getPets(category));
		return "breeds";
	}

	@GetMapping(value = "/breeddetails")
	public String breedeetails(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
			@RequestParam(name = "category") String category, @RequestParam(name = "id") int id)
			throws URISyntaxException {

		if (!"Dog".equals(category) && !"Cat".equals(category) && !"Fish".equals(category)) {
			return "home";
		}

		if (null == this.sessionUser.getPets()) {
			this.petStoreService.getPets(category);
		}

		Pet pet = null;

		try {
			pet = this.sessionUser.getPets().get(id - 1);
		} catch (Exception npe) {
			this.sessionUser.getTelemetryClient().trackException(npe);
			pet = new Pet();
		}

		logger.info(String.format("PetStoreApp /breeddetails requested for %s, routing to dogbreeddetails view...",
				pet.getName()));

		model.addAttribute("pet", pet);

		return "breeddetails";
	}

	@GetMapping(value = "/products")
	public String products(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
			@RequestParam(name = "category") String category, @RequestParam(name = "id") int id)
			throws URISyntaxException {

		if (!"Toy".equals(category) && !"Food".equals(category)) {
			return "home";
		}
		logger.info(String.format("PetStoreApp /products requested for %s, routing to products view...", category));

		Collection<Pet> pets = this.petStoreService.getPets(category);

		Pet pet = new Pet();

		if (pets != null) {
			pet = this.sessionUser.getPets().get(id - 1);
		}

		model.addAttribute("products",
				this.petStoreService.getProducts(pet.getCategory().getName() + " " + category, pet.getTags()));
		return "products";
	}

	@GetMapping(value = "/cart")
	public String cart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
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
		return "cart";
	}

	@PostMapping(value = "/updatecart")
	public String updatecart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
			@RequestParam Map<String, String> params) {
		int cartCount = 1;

		String operator = params.get("operator");
		if (StringUtils.isNotEmpty(operator)) {
			if ("minus".equals(operator)) {
				cartCount = -1;
			}
		}

		this.petStoreService.updateOrder(Long.parseLong(params.get("productId")), cartCount, false);
		return "redirect:cart";
	}

	@PostMapping(value = "/completecart")
	public String updatecart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
		if (token != null) {
			this.petStoreService.updateOrder(0, 0, true);
		}
		return "redirect:cart";
	}

	@GetMapping(value = "/claims")
	public String claims(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
			throws URISyntaxException {
		logger.info(String.format("PetStoreApp /claims requested for %s, routing to claims view...",
				this.sessionUser.getName()));
		return "claims";
	}


	@GetMapping(value = "/*")
	public String landing(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
			throws URISyntaxException {
		logger.info(String.format("PetStoreApp %s requested and %s is being routed to home view session %s",
				request.getRequestURI(), this.sessionUser.getName(), this.sessionUser.getSessionId()));
		PageViewTelemetry pageViewTelemetry = new PageViewTelemetry();
		pageViewTelemetry.setUrl(new URI(request.getRequestURL().toString()));
		pageViewTelemetry.setName("landing");
		this.sessionUser.getTelemetryClient().trackPageView(pageViewTelemetry);
		return "home";
	}
}
