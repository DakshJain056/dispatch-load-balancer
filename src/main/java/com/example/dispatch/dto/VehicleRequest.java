package com.example.dispatch.dto;

import jakarta.validation.constraints.*;

public record VehicleRequest(
    @NotBlank String vehicleId,
    @Positive double capacity,
    @DecimalMin("-90.0") @DecimalMax("90.0") double currentLatitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") double currentLongitude,
    @NotBlank String currentAddress
) {}
