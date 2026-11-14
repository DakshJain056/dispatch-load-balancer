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
@Tag(name="Dispatch",description="Priority-aware delivery allocation and route planning")
public class DispatchController {
    private final DispatchService service;
    public DispatchController(DispatchService service){this.service=service;}

    @PostMapping("/orders")
    @Operation(summary="Create or update orders",description="Upserts by orderId. A repeated ID updates the existing order.")
    public ResponseEntity<ApiMessage> orders(@Valid @RequestBody OrdersRequest request){int n=service.upsertOrders(request.orders());return ResponseEntity.ok(new ApiMessage("Delivery orders accepted.","success",n));}

    @PostMapping("/vehicles")
    @Operation(summary="Create or update vehicles",description="Upserts by vehicleId. A repeated ID updates the existing vehicle.")
    public ResponseEntity<ApiMessage> vehicles(@Valid @RequestBody VehiclesRequest request){int n=service.upsertVehicles(request.vehicles());return ResponseEntity.ok(new ApiMessage("Vehicle details accepted.","success",n));}

    @GetMapping("/plan")
    @Operation(summary="Generate dispatch plan",description="Assigns HIGH before MEDIUM before LOW, respects capacity, and refines routes using Haversine distances.")
    public DispatchPlanResponse plan(){return service.buildPlan();}

    @DeleteMapping("/data")
    @Operation(summary="Clear all orders and vehicles",description="Deletes all stored dispatch data; intended for test/reset use.")
    public ApiMessage clear(){service.clearAll();return new ApiMessage("Dispatch data cleared.","success",0);}
}
