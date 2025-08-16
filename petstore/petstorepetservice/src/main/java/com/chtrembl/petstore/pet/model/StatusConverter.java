package com.chtrembl.petstore.pet.model;

import com.chtrembl.petstore.pet.model.Pet.Status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StatusConverter implements AttributeConverter<Status, String> {

	@Override
    public String convertToDatabaseColumn(Status status) {
        if (status == null) return null;
        return status.getValue();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return Status.fromValue(dbData);
    }
	
}