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

import static com.chtrembl.petstoreapp.config.Constants.AUTH_TYPE;
import static com.chtrembl.petstoreapp.config.Constants.CONTAINER_HOST;
import static com.chtrembl.petstoreapp.config.Constants.IS_AUTHENTICATED;
import static com.chtrembl.petstoreapp.config.Constants.SESSION_ID;
import static com.chtrembl.petstoreapp.config.Constants.USER_EMAIL;
import static com.chtrembl.petstoreapp.config.Constants.USER_NAME;

/**
 * Controller for the PetStore web application.
 * Handles requests for various pages and manages user sessions.
 */

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebAppController {
    private static final String CURRENT_USERS_HUB = "currentUsers";

    private static final String MODEL_APP_VERSION = "appVersion";
    private static final String MODEL_CART_SIZE = "cartSize";
    private static final String MODEL_CLAIMS = "claims";
    private static final String MODEL_CONTAINER_ENVIRONMENT = "containerEnvironment";
    private static final String MODEL_CURRENT_USERS_ON_SITE = "currentUsersOnSite";
    private static final String MODEL_EMAIL = "email";
    private static final String MODEL_ERROR = "error";
    private static final String MODEL_GRANT_TYPE = "grant_type";
    private static final String MODEL_ORDER = "order";
    private static final String MODEL_PET = "pet";
    private static final String MODEL_PETS = "pets";
    private static final String MODEL_PRODUCTS = "products";
    private static final String MODEL_SESSION_ID = "sessionId";
    private static final String MODEL_STACKTRACE = "stacktrace";
    private static final String MODEL_USER = "user";
    private static final String MODEL_USER_LOGGED_IN = "userLoggedIn";
    private static final String MODEL_USER_NAME = "userName";

    private static final String VIEW_HOME = "home";
    private static final String VIEW_LOGIN = "login";
    private static final String VIEW_BREEDS = "breeds";
    private static final String VIEW_BREED_DETAILS = "breeddetails";
    private static final String VIEW_PRODUCTS = "products";
    private static final String VIEW_CART = "cart";
    private static final String VIEW_REDIRECT_CART = "redirect:cart";
    private static final String VIEW_CLAIMS = "claims";

    private final ContainerEnvironment containerEnvironment;
    private final PetStoreFacadeService petStoreService;
    private final User sessionUser;
    private final CacheManager currentUsersCacheManager;

    @ModelAttribute
    public void setModel(HttpServletRequest request, Model model, OAuth2AuthenticationToken token) {

        CaffeineCache caffeineCache = (CaffeineCache) this.currentUsersCacheManager
                .getCache(CURRENT_USERS_HUB);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

        if (sessionUser.getSessionId() == null) {
            String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
            sessionUser.setSessionId(sessionId);
            caffeineCache.put(sessionUser.getSessionId(), sessionUser.getName());
        }

        MDC.put(SESSION_ID, sessionUser.getSessionId());
        MDC.put(USER_NAME, sessionUser.getName());
        MDC.put(CONTAINER_HOST, this.containerEnvironment.getContainerHostName());

        caffeineCache.put(sessionUser.getSessionId(), sessionUser.getName());

        if (token != null) {
            final OAuth2User user = token.getPrincipal();

            String userEmail = extractUserEmail(user);
            if (userEmail != null) {
                sessionUser.setEmail(userEmail);
                MDC.put(USER_EMAIL, userEmail);
                log.debug("User email set to: {}", userEmail);
            } else {
                log.warn("Could not extract email for user: {}", sessionUser.getName());
            }

            sessionUser.setName((String) user.getAttributes().get("name"));
            MDC.put(USER_NAME, sessionUser.getName());
            MDC.put(AUTH_TYPE, "OAuth2");
            MDC.put(IS_AUTHENTICATED, "true");

            if (!sessionUser.isInitialTelemetryRecorded()) {
                sessionUser.getTelemetryClient().trackEvent(
                        String.format("PetStoreApp %s logged in, container host: %s", sessionUser.getName(),
                                this.containerEnvironment.getContainerHostName()),
                        sessionUser.getCustomEventProperties(), null);

                sessionUser.setInitialTelemetryRecorded(true);
            }
            model.addAttribute(MODEL_CLAIMS, user.getAttributes());
            model.addAttribute(MODEL_USER, sessionUser.getName());
            model.addAttribute(MODEL_GRANT_TYPE, user.getAuthorities());
        } else {
            MDC.put(AUTH_TYPE, "Anonymous");
            MDC.put(IS_AUTHENTICATED, "false");
        }

        model.addAttribute(MODEL_USER_NAME, sessionUser.getName());
        model.addAttribute(MODEL_CONTAINER_ENVIRONMENT, this.containerEnvironment);
        model.addAttribute(MODEL_SESSION_ID, sessionUser.getSessionId());
        model.addAttribute(MODEL_APP_VERSION, this.containerEnvironment.getAppVersion());
        model.addAttribute(MODEL_CART_SIZE, sessionUser.getCartCount());
        model.addAttribute(MODEL_CURRENT_USERS_ON_SITE, nativeCache.asMap().size());
    }

    @GetMapping(value = "/login")
    public String login(Model model, HttpServletRequest request) throws URISyntaxException {
        log.debug("PetStoreApp /login requested, routing to login view...");

        PageViewTelemetry pageViewTelemetry = new PageViewTelemetry();
        pageViewTelemetry.setUrl(new URI(request.getRequestURL().toString()));
        pageViewTelemetry.setName("login");
        sessionUser.getTelemetryClient().trackPageView(pageViewTelemetry);
        return VIEW_LOGIN;
    }

    @GetMapping(value = {"/dogbreeds", "/catbreeds", "/fishbreeds"})
    public String breeds(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
                         @RequestParam(name = "category") String category) throws URISyntaxException {
        if (!"Dog".equals(category) && !"Cat".equals(category) && !"Fish".equals(category)) {
            return VIEW_HOME;
        }
        try {
            final Collection<Pet> pets = this.petStoreService.getPets(category);
            model.addAttribute(MODEL_PETS, pets);
        } catch (Exception ex) {
            log.error("Error loading pets from service: ", ex);
            model.addAttribute(MODEL_ERROR, "Sorry, we couldn't load pet breeds.");
            model.addAttribute(MODEL_STACKTRACE, getStackTrace(ex));
        }
        return VIEW_BREEDS;
    }

    @GetMapping(value = "/breeddetails")
    public String breedeetails(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
                               @RequestParam(name = "category") String category,
                               @RequestParam(name = "id") int id) throws URISyntaxException {

        if (!"Dog".equals(category) && !"Cat".equals(category) && !"Fish".equals(category)) {
            return VIEW_HOME;
        }

        try {
            if (sessionUser.getPets() == null) {
                this.petStoreService.getPets(category);
            }

            Pet pet = sessionUser.getPets().get(id - 1);
            log.debug("PetStoreApp /breeddetails requested for {}, routing to dogbreeddetails view...", pet.getName());
            model.addAttribute(MODEL_PET, pet);

        } catch (Exception ex) {
            log.error("Error loading pet details: ", ex);
            model.addAttribute(MODEL_ERROR, "Sorry, we couldn't load pet details.");
            model.addAttribute(MODEL_STACKTRACE, getStackTrace(ex));
        }

        return VIEW_BREED_DETAILS;
    }

    @GetMapping(value = "/products")
    public String products(Model model, OAuth2AuthenticationToken token, HttpServletRequest request,
                           @RequestParam(name = "category") String category,
                           @RequestParam(name = "id") int id) throws URISyntaxException {

        if (!"Toy".equals(category) && !"Food".equals(category)) {
            return VIEW_HOME;
        }

        try {
            log.debug("PetStoreApp /products requested for {}, routing to products view...", category);
            Pet pet = sessionUser.getPets().get(id - 1);
            model.addAttribute(MODEL_PRODUCTS,
                    this.petStoreService.getProducts(pet.getCategory().getName() + " " + category, pet.getTags()));
        } catch (Exception ex) {
            log.error("Error loading products: ", ex);
            model.addAttribute(MODEL_ERROR, "Sorry, we couldn't load products.");
            model.addAttribute(MODEL_STACKTRACE, getStackTrace(ex));
        }

        return VIEW_PRODUCTS;
    }

    @GetMapping(value = "/cart")
    public String cart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
        try {
            Order order = this.petStoreService.retrieveOrder(sessionUser.getSessionId());
            model.addAttribute(MODEL_ORDER, order);

            int cartSize = 0;
            if (order != null && order.getProducts() != null && !order.isComplete()) {
                cartSize = order.getProducts().size();
            }
            sessionUser.setCartCount(cartSize);
            model.addAttribute(MODEL_CART_SIZE, sessionUser.getCartCount());

            if (token != null) {
                model.addAttribute(MODEL_USER_LOGGED_IN, true);
                model.addAttribute(MODEL_EMAIL, sessionUser.getEmail());
            }

        } catch (Exception ex) {
            log.error("Error loading cart: ", ex);
            model.addAttribute(MODEL_ERROR, "Sorry, we couldn't load your cart.");
            model.addAttribute(MODEL_STACKTRACE, getStackTrace(ex));
        }

        return VIEW_CART;
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
        return VIEW_REDIRECT_CART;
    }

    @PostMapping(value = "/completecart")
    public String updateCart(Model model, OAuth2AuthenticationToken token, HttpServletRequest request) {
        if (token != null) {
            this.petStoreService.updateOrder(0, 0, true);
        }
        return VIEW_REDIRECT_CART;
    }

    @GetMapping(value = "/claims")
    public String claims(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
            throws URISyntaxException {
        log.debug("PetStoreApp /claims requested for {}, routing to claims view...", sessionUser.getName());
        return VIEW_CLAIMS;
    }

    @GetMapping("/fail")
    public String fail() {
        throw new RuntimeException("Test 500 error");
    }

    @GetMapping(value = {"/", "/home.htm*", "/index.htm*"})
    public String landing(Model model, OAuth2AuthenticationToken token, HttpServletRequest request)
            throws URISyntaxException {
        return VIEW_HOME;
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
