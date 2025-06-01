package com.chtrembl.petstore.pet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Version implements Serializable {
	private String version;
	private String date;
}
