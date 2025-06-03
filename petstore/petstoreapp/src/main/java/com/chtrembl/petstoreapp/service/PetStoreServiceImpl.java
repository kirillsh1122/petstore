package com.chtrembl.petstoreapp.service;

import com.chtrembl.petstoreapp.client.OrderServiceClient;
import com.chtrembl.petstoreapp.client.PetServiceClient;
import com.chtrembl.petstoreapp.client.ProductServiceClient;
import com.chtrembl.petstoreapp.model.Category;
import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Order;
import com.chtrembl.petstoreapp.model.Pet;
import com.chtrembl.petstoreapp.model.Product;
import com.chtrembl.petstoreapp.model.Tag;
import com.chtrembl.petstoreapp.model.User;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetStoreServiceImpl implements PetStoreService {

	private final User sessionUser;
	private final ContainerEnvironment containerEnvironment;
	private final PetServiceClient petServiceClient;
	private final ProductServiceClient productServiceClient;
	private final OrderServiceClient orderServiceClient;

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

			pets = petServiceClient.getPetsByStatus("available");
			this.sessionUser.setPets(pets);

			pets = pets.stream()
					.filter(pet -> category.equals(pet.getCategory().getName()))
					.collect(Collectors.toList());

			log.info("Successfully retrieved {} pets for category {}", pets.size(), category);

			return pets;
		} catch (FeignException fe) {
			this.sessionUser.getTelemetryClient().trackException(fe);
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp %s received Feign error %s (HTTP %d), container host: %s",
							this.sessionUser.getName(),
							fe.getMessage(),
							fe.status(),
							this.containerEnvironment.getContainerHostName())
			);
			log.error("Failed to retrieve pets from PetStorePetService via Feign client", fe);
			throw new IllegalStateException("Unable to retrieve pets from the PetStorePetService", fe);
		} catch (Exception e) {
			log.error("Unexpected error when retrieving pets", e);

			// Return error pet for display
			Pet errorPet = new Pet();
			errorPet.setName("petstore.service.url:${PETSTOREPETSERVICE_URL} needs to be enabled for this service to work: "
					+ e.getMessage());
			errorPet.setPhotoURL("");
			errorPet.setCategory(new Category());
			errorPet.setId((long) 0);
			pets.add(errorPet);
		} finally {
			MDC.remove("operation");
			MDC.remove("category");
		}
		return pets;
	}

	@Override
	public Collection<Product> getProducts(String category, List<Tag> tags) {
		List<Product> products;

		MDC.put("operation", "getProducts");
		MDC.put("category", category);

		try {
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp user %s is requesting to retrieve products from the ProductService",
							this.sessionUser.getName()),
					this.sessionUser.getCustomEventProperties(), null);

			products = productServiceClient.getProductsByStatus("available");
			this.sessionUser.setProducts(products);

			if (tags.stream().anyMatch(t -> t.getName().equals("large"))) {
				products = products.stream()
						.filter(product -> category.equals(product.getCategory().getName())
								&& product.getTags().toString().contains("large"))
						.collect(Collectors.toList());
			} else {
				products = products.stream()
						.filter(product -> category.equals(product.getCategory().getName())
								&& product.getTags().toString().contains("small"))
						.toList();
			}

			log.info("Successfully retrieved {} products for category {} with tags {}",
					products.size(), category, tags);

			return products;
		} catch (FeignException fe) {
			this.sessionUser.getTelemetryClient().trackException(fe);
			this.sessionUser.getTelemetryClient().trackEvent(
					String.format("PetStoreApp %s received Feign error %s (HTTP %d), container host: %s",
							this.sessionUser.getName(),
							fe.getMessage(),
							fe.status(),
							this.containerEnvironment.getContainerHostName())
			);
			log.error("Failed to retrieve products from ProductService via Feign client", fe);
			throw new IllegalStateException("Unable to retrieve products from product service", fe);
		} finally {
			MDC.remove("operation");
			MDC.remove("category");
		}
	}

	@Override
	public void updateOrder(long productId, int quantity, boolean completeOrder) {
		MDC.put("operation", "updateOrder");
		MDC.put("productId", String.valueOf(productId));
		MDC.put("quantity", String.valueOf(quantity));
		MDC.put("completeOrder", String.valueOf(completeOrder));

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
				log.debug("Setting order email to: {}", userEmail);
			} else {
				log.warn("User email is not available for session: {}", this.sessionUser.getSessionId());
			}

			if (completeOrder) {
				updatedOrder.setComplete(true);
				log.info("Completing order for session: {}", this.sessionUser.getSessionId());
			} else {
				List<Product> products = new ArrayList<>();
				Product product = new Product();
				product.setId(productId);
				product.setQuantity(quantity);
				products.add(product);
				updatedOrder.setProducts(products);
				log.debug("Adding/updating product {} with quantity {} to order", productId, quantity);
			}

			// Serialize order to JSON
			String orderJSON = new ObjectMapper()
					.setSerializationInclusion(Include.NON_NULL)
					.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
					.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
					.writeValueAsString(updatedOrder);

			Order resultOrder = orderServiceClient.createOrUpdateOrder(orderJSON);
			log.info("Successfully updated order via Feign client: {}", resultOrder.getId());

		} catch (FeignException fe) {
			log.error("Unable to update order via Feign client: HTTP {} - {}", fe.status(), fe.getMessage(), fe);
			this.sessionUser.getTelemetryClient().trackException(fe);
			throw new IllegalStateException("Unable to update order via order service", fe);
		} catch (Exception e) {
			log.error("Unexpected error updating order", e);
			this.sessionUser.getTelemetryClient().trackException(e);
			throw new IllegalStateException("Unable to update order via order service", e);
		} finally {
			MDC.remove("operation");
			MDC.remove("productId");
			MDC.remove("quantity");
			MDC.remove("completeOrder");
		}
	}

	@Override
	public Order retrieveOrder(String orderId) {
		MDC.put("operation", "retrieveOrder");
		MDC.put("orderId", orderId);

		this.sessionUser.getTelemetryClient()
				.trackEvent(String.format(
						"PetStoreApp user %s is requesting to retrieve an order from the PetStoreOrderService",
						this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);

		try {
			Order order = orderServiceClient.getOrder(orderId);
			log.debug("Successfully retrieved order: {}", orderId);
			return order;

		} catch (FeignException.NotFound e) {
			log.debug("Order not found: {}", orderId);
			return null;
		} catch (FeignException fe) {
			log.error("Unable to retrieve order via Feign client: HTTP {} - {}", fe.status(), fe.getMessage(), fe);
			this.sessionUser.getTelemetryClient().trackException(fe);
			throw new IllegalStateException("Unable to retrieve order from order service", fe);
		} catch (Exception e) {
			log.error("Unexpected error retrieving order: {}", orderId, e);
			this.sessionUser.getTelemetryClient().trackException(e);
			throw new IllegalStateException("Unable to retrieve order from order service", e);
		} finally {
			MDC.remove("operation");
			MDC.remove("orderId");
		}
	}
}
