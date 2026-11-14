package com.example.dispatch.dto;

import jakarta.validation.constraints.*;

public record VehicleRequest(
    @NotBlank @Size(max=64) String vehicleId,
    @NotNull @Positive Double capacity,
    @NotNull @DecimalMin(value="-90.0") @DecimalMax(value="90.0") Double currentLatitude,
    @NotNull @DecimalMin(value="-180.0") @DecimalMax(value="180.0") Double currentLongitude,
    @NotBlank @Size(max=500) String currentAddress
) {}
