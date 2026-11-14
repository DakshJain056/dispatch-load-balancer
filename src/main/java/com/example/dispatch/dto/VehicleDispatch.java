package com.example.dispatch.dto;
import java.util.List;
public record VehicleDispatch(String vehicleId, double capacity, double currentLatitude, double currentLongitude,
                              String currentAddress, double totalLoad, String totalDistance,
                              double totalDistanceKm, List<AssignedOrder> assignedOrders) {}
