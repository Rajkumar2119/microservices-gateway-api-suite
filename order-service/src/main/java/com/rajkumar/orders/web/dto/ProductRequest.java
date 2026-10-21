package com.rajkumar.orders.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9-]+$", message = "use upper-case letters, digits and '-'")
        String sku,
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal priceGbp,
        Boolean active) {
}
