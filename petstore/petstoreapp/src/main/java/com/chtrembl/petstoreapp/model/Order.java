package com.chtrembl.petstoreapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Order
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Order implements Serializable {
	private String id;
	private String email;
	private List<Product> products;
	private OffsetDateTime shipDate;
	private StatusEnum status;

	public boolean isComplete() {
		return complete != null ? complete : false;
	}

	private Boolean complete = false;

	/**
	 * Order Status
	 */
	public enum StatusEnum {
		PLACED("placed"),
		APPROVED("approved"),
		DELIVERED("delivered");

		private final String value;

		StatusEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static StatusEnum fromValue(String text) {
			for (StatusEnum b : StatusEnum.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	}

	public Order id(String id) {
		this.id = id;
		return this;
	}

	public Order products(List<Product> products) {
		this.products = products;
		return this;
	}

	public Order complete(Boolean complete) {
		this.complete = complete;
		return this;
	}
}
