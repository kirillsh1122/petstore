package com.chtrembl.petstore.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelApiResponse {
    private Integer code;
    private String type;
    private String message;
}
