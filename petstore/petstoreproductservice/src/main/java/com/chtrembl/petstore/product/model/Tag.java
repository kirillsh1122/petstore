package com.chtrembl.petstore.product.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name="tag", schema="public")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
    private String name;

    public Tag name(String name) {
        this.name = name;
        return this;
    }
}