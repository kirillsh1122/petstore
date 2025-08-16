package com.chtrembl.petstore.product.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@Data
public class DataPreload {
	private final ProductRepository productRepository;
	private List<Product> products;
	
	public DataPreload(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
	
	@PostConstruct
    public void loadData() {
		this.products = (List<Product>) productRepository.findAll();
    }
}
