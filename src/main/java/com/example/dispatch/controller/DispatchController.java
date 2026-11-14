package com.example.dispatch.controller;

import com.example.dispatch.dto.*;
import com.example.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
@Tag(name = "Dispatch", description = "Delivery order and fleet dispatch operations")
public class DispatchController {
    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) { this.dispatchService = dispatchService; }

    @PostMapping("/orders")
    @Operation(summary = "Create or update delivery orders", description = "Upserts orders by orderId.")
    public ResponseEntity<ApiMessage> acceptOrders(@Valid @RequestBody OrdersRequest request) {
        dispatchService.upsertOrders(request.orders());
        return ResponseEntity.ok(new ApiMessage("Delivery orders accepted.", "success"));
    }

    @PostMapping("/vehicles")
    @Operation(summary = "Create or update fleet vehicles", description = "Upserts vehicles by vehicleId.")
    public ResponseEntity<ApiMessage> acceptVehicles(@Valid @RequestBody VehiclesRequest request) {
        dispatchService.upsertVehicles(request.vehicles());
        return ResponseEntity.ok(new ApiMessage("Vehicle details accepted.", "success"));
    }

    @GetMapping("/plan")
    @Operation(summary = "Retrieve optimized dispatch plan", description = "HIGH priority orders are assigned before MEDIUM and LOW.")
    public DispatchPlanResponse getPlan() { return dispatchService.buildPlan(); }

    @DeleteMapping("/data")
    @Operation(summary = "Clear in-memory orders and vehicles", description = "Useful for resetting local/demo data.")
    public ApiMessage clearData() {
        dispatchService.clearAll();
        return new ApiMessage("Dispatch data cleared.", "success");
    }
}
