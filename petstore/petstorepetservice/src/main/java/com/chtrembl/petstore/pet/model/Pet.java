package com.chtrembl.petstore.pet.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name="pet", schema="public")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Valid
    @ManyToOne
    private Category category;

    @NotNull
    private String name;

    @JsonProperty("photoURL")
    @NotNull
    private String photoURL;

    @Valid
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
    		name = "pet_tag",
    		joinColumns=
    			@JoinColumn(name="pet_id"),
    		inverseJoinColumns=
            	@JoinColumn(name="tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @Convert(converter = StatusConverter.class)
    private Status status;

    public Pet name(String name) {
        this.name = name;
        return this;
    }

    public enum Status {
        AVAILABLE("available"),
        PENDING("pending"),
        SOLD("sold");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            for (Status b : Status.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
