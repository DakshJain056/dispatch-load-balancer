package com.example.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrdersRequest(@NotEmpty List<@Valid OrderRequest> orders) {}
