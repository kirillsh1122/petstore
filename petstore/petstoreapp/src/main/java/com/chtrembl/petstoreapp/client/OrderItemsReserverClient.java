package com.chtrembl.petstoreapp.client;

import com.chtrembl.petstoreapp.config.FeignConfig;
import com.chtrembl.petstoreapp.model.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "reserver-service",
        url = "${petstore.service.orderreserver.url}",
        configuration = FeignConfig.class
)
public interface OrderItemsReserverClient {
	
	@PostMapping("/api/HTTPExample")
    Order createOrUpdateOrder(@RequestBody String orderJson);

}
