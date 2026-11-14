package com.example.dispatch.dto;
import java.util.List;
public record DispatchPlanResponse(List<VehicleDispatch> dispatchPlan, List<UnassignedOrder> unassignedOrders,
                                   double totalAssignedLoad, double totalDistanceKm, int assignedOrderCount,
                                   int unassignedOrderCount, String optimizationMethod, String distanceDefinition) {}
