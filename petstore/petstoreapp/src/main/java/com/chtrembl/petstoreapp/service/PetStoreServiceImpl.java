package com.chtrembl.petstoreapp.service;

import com.chtrembl.petstoreapp.model.Category;
import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Order;
import com.chtrembl.petstoreapp.model.Pet;
import com.chtrembl.petstoreapp.model.Product;
import com.chtrembl.petstoreapp.model.Tag;
import com.chtrembl.petstoreapp.model.User;
import com.chtrembl.petstoreapp.model.WebRequest;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetStoreServiceImpl implements PetStoreService {
	private static final Logger logger = LoggerFactory.getLogger(PetStoreServiceImpl.class);

	private final User sessionUser;
	private final ContainerEnvironment containerEnvironment;
	private final WebRequest webRequest;

	private WebClient petServiceWebClient = null;
	private WebClient productServiceWebClient = null;
	private WebClient orderServiceWebClient = null;

	@PostConstruct
	public void initialize() {
		this.petServiceWebClient = WebClient.builder()
				.baseUrl(this.containerEnvironment.getPetStorePetServiceURL())
				.build();
		this.productServiceWebClient = WebClient.builder()
				.baseUrl(this.containerEnvironment.getPetStoreProductServiceURL()).build();
		this.orderServiceWebClient = WebClient.builder().baseUrl(this.containerEnvironment.getPetStoreOrderServiceURL())
				.build();
	}

	@Override
	public Collection<Pet> getPets(String category) {
		List<Pet> pets = new ArrayList<>();

		MDC.put("operation", "getPets");
		MDC.put("category", category);

		try {
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp user %s is requesting to retrieve pets from the PetStorePetService",
							this.sessionUser.getName()),
					this.sessionUser.getCustomEventProperties(), null);

			Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());
			pets = this.petServiceWebClient.get().uri("petstorepetservice/v2/pet/findByStatus?status=available")
					.accept(MediaType.APPLICATION_JSON)
					.headers(consumer)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Cache-Control", "no-cache")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<List<Pet>>() {
					}).block();
			this.sessionUser.setPets(pets);

			pets = pets.stream().filter(pet -> category.equals(pet.getCategory().getName()))
					.collect(Collectors.toList());

			logger.info("Successfully retrieved {} pets for category {}", pets.size(), category);

			return pets;
		} catch (WebClientException wce) {
			this.sessionUser.getTelemetryClient().trackException(wce);
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp %s received %s, container host: %s",
							this.sessionUser.getName(),
							wce.getMessage(),
							this.containerEnvironment.getContainerHostName())
			);
			logger.error("Failed to retrieve pets from PetStorePetService", wce);
			throw new IllegalStateException("Unable to retrieve pets from the PetStorePetService", wce);
		} catch (IllegalArgumentException iae) {
			logger.error("Invalid argument when retrieving pets", iae);

			Pet pet = new Pet();
			pet.setName("petstore.service.url:${PETSTOREPETSERVICE_URL} needs to be enabled for this service to work"
					+ iae.getMessage());
			pet.setPhotoURL("");
			pet.setCategory(new Category());
			pet.setId((long) 0);
			pets.add(pet);
		} finally {
			MDC.remove("operation");
			MDC.remove("category");
		}
		return pets;
	}

	@Override
	public Collection<Product> getProducts(String category, List<Tag> tags) {
		List<Product> products;

		try {
			Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());
			products = this.productServiceWebClient.get()
					.uri("petstoreproductservice/v2/product/findByStatus?status=available")
					.accept(MediaType.APPLICATION_JSON)
					.headers(consumer)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Cache-Control", "no-cache")
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<List<Product>>() {
					}).block();

			this.sessionUser.setProducts(products);

			if (tags.stream().anyMatch(t -> t.getName().equals("large"))) {
				products = products.stream().filter(product -> category.equals(product.getCategory().getName())
						&& product.getTags().toString().contains("large")).collect(Collectors.toList());
			} else {

				products = products.stream().filter(product -> category.equals(product.getCategory().getName())
						&& product.getTags().toString().contains("small")).collect(Collectors.toList());
			}
			return products;
		} catch (WebClientException | IllegalArgumentException wce) {
			this.sessionUser.getTelemetryClient().trackException(wce);
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp %s received %s, container host: %s",
							this.sessionUser.getName(),
							wce.getMessage(),
							this.containerEnvironment.getContainerHostName())
			);
			throw new IllegalStateException("Unable to retrieve products from product service", wce);
		}
	}

	@Override
	public void updateOrder(long productId, int quantity, boolean completeOrder) {
		this.sessionUser.getTelemetryClient()
				.trackEvent(String.format(
						"PetStoreApp user %s is trying to update an order",
						this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);

		try {
			Order updatedOrder = new Order();

			updatedOrder.setId(this.sessionUser.getSessionId());

			// Set email only if user is logged in and email is available
			String userEmail = this.sessionUser.getEmail();
			if (userEmail != null && !userEmail.trim().isEmpty()) {
				updatedOrder.setEmail(userEmail);
				logger.debug("Setting order email to: {}", userEmail);
			} else {
				logger.warn("User email is not available for session: {}", this.sessionUser.getSessionId());
			}

			if (completeOrder) {
				updatedOrder.setComplete(true);
			} else {
				List<Product> products = new ArrayList<>();
				Product product = new Product();
				product.setId(productId);
				product.setQuantity(quantity);
				products.add(product);
				updatedOrder.setProducts(products);
			}

			String orderJSON = new ObjectMapper().setSerializationInclusion(Include.NON_NULL)
					.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
					.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false).writeValueAsString(updatedOrder);

			Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());

			updatedOrder = this.orderServiceWebClient.post().uri("petstoreorderservice/v2/store/order")
					.body(BodyInserters.fromPublisher(Mono.just(orderJSON), String.class))
					.accept(MediaType.APPLICATION_JSON)
					.headers(consumer)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Cache-Control", "no-cache")
					.retrieve()
					.bodyToMono(Order.class).block();
		} catch (Exception e) {
			logger.error("Unable to retrieve order from order service", e);
			throw new IllegalStateException("Unable to retrieve order from order service", e);
		}
	}

	@Override
	public Order retrieveOrder(String orderId) {
		this.sessionUser.getTelemetryClient()
				.trackEvent(String.format(
						"PetStoreApp user %s is requesting to retrieve an order from the PetStoreOrderService",
						this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);

		Order order = null;
		try {
			Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());

			order = this.orderServiceWebClient.get()
					.uri(uriBuilder -> uriBuilder.path("petstoreorderservice/v2/store/order/{orderId}").build(orderId))
					.accept(MediaType.APPLICATION_JSON)
					.headers(consumer)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Cache-Control", "no-cache")
					.retrieve()
					.onStatus(
							status -> status.value() == 404,
							response -> Mono.empty()
					)
					.bodyToMono(new ParameterizedTypeReference<Order>() {
					})
					.block();

			return order;

		} catch (Exception e) {
			logger.error("Unable to retrieve order from order service", e);
			throw new IllegalStateException("Unable to retrieve order from order service", e);
		}
	}

}
