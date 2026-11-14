package com.example.dispatch.service;

import com.example.dispatch.dto.*;
import com.example.dispatch.model.Priority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DispatchService {
    private final DistanceCalculator distanceCalculator;
    private final Map<String, OrderRequest> orders = new ConcurrentHashMap<>();
    private final Map<String, VehicleRequest> vehicles = new ConcurrentHashMap<>();

    public DispatchService(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public synchronized void upsertOrders(List<OrderRequest> incoming) {
        Set<String> ids = new HashSet<>();
        for (OrderRequest order : incoming) {
            if (!ids.add(order.orderId())) throw new IllegalArgumentException("Duplicate orderId in request: " + order.orderId());
        }
        incoming.forEach(o -> orders.put(o.orderId(), o));
    }

    public synchronized void upsertVehicles(List<VehicleRequest> incoming) {
        Set<String> ids = new HashSet<>();
        for (VehicleRequest vehicle : incoming) {
            if (!ids.add(vehicle.vehicleId())) throw new IllegalArgumentException("Duplicate vehicleId in request: " + vehicle.vehicleId());
        }
        incoming.forEach(v -> vehicles.put(v.vehicleId(), v));
    }

    public synchronized DispatchPlanResponse buildPlan() {
        List<OrderRequest> sortedOrders = new ArrayList<>(orders.values());
        sortedOrders.sort(Comparator.comparingInt((OrderRequest o) -> priorityRank(o.priority()))
                .thenComparing(OrderRequest::orderId));

        List<VehicleState> states = vehicles.values().stream()
                .sorted(Comparator.comparing(VehicleRequest::vehicleId))
                .map(VehicleState::new).collect(Collectors.toCollection(ArrayList::new));

        List<UnassignedOrder> unassigned = new ArrayList<>();
        for (OrderRequest order : sortedOrders) {
            VehicleState best = states.stream()
                    .filter(v -> v.remainingCapacity + 1e-9 >= order.packageWeight())
                    .min(Comparator.comparingDouble(v -> distanceCalculator.haversineKm(
                            v.lastLat, v.lastLon, order.latitude(), order.longitude())))
                    .orElse(null);
            if (best == null) {
                String reason = states.isEmpty() ? "No vehicles have been registered"
                        : states.stream().noneMatch(v -> v.vehicle.capacity() + 1e-9 >= order.packageWeight())
                        ? "Order weight exceeds every vehicle's capacity" : "Insufficient remaining fleet capacity";
                unassigned.add(new UnassignedOrder(order.orderId(), reason));
                continue;
            }
            best.assign(order, distanceCalculator);
        }

        List<VehicleDispatch> plan = states.stream().map(VehicleState::toDispatch).toList();
        return new DispatchPlanResponse(plan, unassigned);
    }

    public synchronized void clearAll() {
        orders.clear();
        vehicles.clear();
    }

    private int priorityRank(Priority p) {
        return switch (p) { case HIGH -> 0; case MEDIUM -> 1; case LOW -> 2; };
    }

    private static class VehicleState {
        final VehicleRequest vehicle;
        final List<OrderRequest> assigned = new ArrayList<>();
        double remainingCapacity, totalLoad, distance, lastLat, lastLon;

        VehicleState(VehicleRequest vehicle) {
            this.vehicle = vehicle;
            this.remainingCapacity = vehicle.capacity();
            this.lastLat = vehicle.currentLatitude();
            this.lastLon = vehicle.currentLongitude();
        }

        void assign(OrderRequest order, DistanceCalculator calculator) {
            distance += calculator.haversineKm(lastLat, lastLon, order.latitude(), order.longitude());
            lastLat = order.latitude();
            lastLon = order.longitude();
            totalLoad += order.packageWeight();
            remainingCapacity -= order.packageWeight();
            assigned.add(order);
        }

        VehicleDispatch toDispatch() {
            return new VehicleDispatch(vehicle.vehicleId(), round(totalLoad),
                    String.format(Locale.ROOT, "%.2f km", distance), List.copyOf(assigned));
        }

        private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
    }
}
