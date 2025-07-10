package com.chtrembl.petstore.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.chtrembl.petstore.product.model.DataPreload;
import com.chtrembl.petstore.product.model.ProductRepository;

@SpringBootApplication
public class ProductServiceApplication {
	
	@Bean
	public DataPreload dataPreload(ProductRepository productRepository) {
		return new DataPreload(productRepository);
	}

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}