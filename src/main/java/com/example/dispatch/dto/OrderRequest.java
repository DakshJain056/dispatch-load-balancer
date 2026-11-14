package com.example.dispatch.dto;

import jakarta.validation.constraints.*;

public record OrderRequest(
    @NotBlank @Size(max=64) String orderId,
    @NotNull @DecimalMin(value="-90.0") @DecimalMax(value="90.0") Double latitude,
    @NotNull @DecimalMin(value="-180.0") @DecimalMax(value="180.0") Double longitude,
    @NotBlank @Size(max=500) String address,
    @NotNull @Positive Double packageWeight,
    @NotNull Priority priority
) {}
