package com.example.dispatch.service;

import com.example.dispatch.dto.*;
import com.example.dispatch.model.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DispatchServiceTest {
    private DispatchService service;

    @BeforeEach void setUp() { service = new DispatchService(new DistanceCalculator()); }

    private OrderRequest order(String id, double weight, Priority priority, double lat, double lon) {
        return new OrderRequest(id, lat, lon, "Test address", weight, priority);
    }
    private VehicleRequest vehicle(String id, double capacity) {
        return new VehicleRequest(id, capacity, 28.6139, 77.2090, "Delhi");
    }

    @Test void assignsOrdersWithoutExceedingCapacity() {
        service.upsertVehicles(List.of(vehicle("V1", 20)));
        service.upsertOrders(List.of(order("O1", 10, Priority.HIGH, 28.61, 77.20),
                order("O2", 15, Priority.LOW, 28.62, 77.21)));
        DispatchPlanResponse result = service.buildPlan();
        assertEquals(10.0, result.dispatchPlan().get(0).totalLoad());
        assertEquals(1, result.unassignedOrders().size());
        assertEquals("O2", result.unassignedOrders().get(0).orderId());
    }

    @Test void highPriorityIsAssignedBeforeLowPriority() {
        service.upsertVehicles(List.of(vehicle("V1", 10)));
        service.upsertOrders(List.of(order("LOW", 10, Priority.LOW, 28.62, 77.21),
                order("HIGH", 10, Priority.HIGH, 28.62, 77.21)));
        DispatchPlanResponse result = service.buildPlan();
        assertEquals("HIGH", result.dispatchPlan().get(0).assignedOrders().get(0).orderId());
        assertEquals("LOW", result.unassignedOrders().get(0).orderId());
    }

    @Test void reportsOrdersWhenNoVehiclesExist() {
        service.upsertOrders(List.of(order("O1", 5, Priority.MEDIUM, 28.6, 77.2)));
        assertEquals(1, service.buildPlan().unassignedOrders().size());
    }

    @Test void upsertsExistingOrderById() {
        service.upsertVehicles(List.of(vehicle("V1", 50)));
        service.upsertOrders(List.of(order("O1", 10, Priority.LOW, 28.6, 77.2)));
        service.upsertOrders(List.of(order("O1", 20, Priority.HIGH, 28.6, 77.2)));
        assertEquals(20.0, service.buildPlan().dispatchPlan().get(0).totalLoad());
    }

    @Test void rejectsDuplicateIdsWithinSinglePayload() {
        OrderRequest o = order("O1", 5, Priority.HIGH, 28.6, 77.2);
        assertThrows(IllegalArgumentException.class, () -> service.upsertOrders(List.of(o, o)));
    }

    @Test void emptyFleetProducesEmptyPlanAndUnassignedOrder() {
        service.upsertOrders(List.of(order("O1", 5, Priority.HIGH, 28.6, 77.2)));
        assertTrue(service.buildPlan().dispatchPlan().isEmpty());
        assertEquals(1, service.buildPlan().unassignedOrders().size());
    }
}
