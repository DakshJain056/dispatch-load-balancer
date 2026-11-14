package com.example.dispatch.dto;

import com.example.dispatch.model.Priority;
import jakarta.validation.constraints.*;

public record OrderRequest(
    @NotBlank String orderId,
    @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
    @NotBlank String address,
    @Positive double packageWeight,
    @NotNull Priority priority
) {}
