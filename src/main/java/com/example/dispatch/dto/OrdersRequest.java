package com.example.dispatch.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record OrdersRequest(@NotEmpty @Size(max=500) List<@Valid OrderRequest> orders) {}
