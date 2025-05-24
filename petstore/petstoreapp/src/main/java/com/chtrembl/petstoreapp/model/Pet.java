package com.chtrembl.petstoreapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Pet model representing animals available in the store.
 */
@Data
public class Pet implements Serializable {
	private Long id;
	private Category category;
	private String name;
	private String photoURL;
	private List<Tag> tags;
	private StatusEnum status;

	public enum StatusEnum {
		AVAILABLE("available"),
		PENDING("pending"),
		SOLD("sold");

		private final String value;

		StatusEnum(String value) {
			this.value = value;
		}

		@JsonValue
		public String getValue() {
			return value;
		}

		@JsonCreator
		public static StatusEnum fromValue(String value) {
			for (StatusEnum status : StatusEnum.values()) {
				if (status.value.equals(value)) {
					return status;
				}
			}
			throw new IllegalArgumentException("Unexpected value: " + value);
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
