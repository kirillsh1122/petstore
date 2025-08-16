package com.chtrembl.petstore.pet.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@Data
public class DataPreload{
	
	private final PetRepository petRepository;
	private List<Pet> pets;
	
	public DataPreload(PetRepository petRepository) {
        this.petRepository = petRepository;
    }
	
	@PostConstruct
    public void loadData() {
		this.pets = (List<Pet>) petRepository.findAll();
    }
}
