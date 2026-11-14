package com.example.dispatch.service;

import com.example.dispatch.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OriginalSampleDatasetTest {
    @Autowired DispatchService service;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach void clear(){service.clearAll();}
    @AfterEach void clearAfter(){service.clearAll();}

    @Test void originalThirtyOrderFiveVehicleDatasetRespectsAllConstraints() throws Exception {
        VehiclesRequest vehicleRequest=objectMapper.readValue(new ClassPathResource("sample-data/vehicles-5.json").getInputStream(),VehiclesRequest.class);
        OrdersRequest orderRequest=objectMapper.readValue(new ClassPathResource("sample-data/orders-30.json").getInputStream(),OrdersRequest.class);
        service.upsertVehicles(vehicleRequest.vehicles());service.upsertOrders(orderRequest.orders());
        DispatchPlanResponse plan=service.buildPlan();

        assertEquals(30,plan.assignedOrderCount()+plan.unassignedOrderCount());
        assertTrue(plan.dispatchPlan().stream().allMatch(v->v.totalLoad()<=v.capacity()+1e-9));
        Set<String> assignedIds=new HashSet<>();
        plan.dispatchPlan().forEach(v->v.assignedOrders().forEach(o->assignedIds.add(o.orderId())));
        long highAndMedium=orderRequest.orders().stream().filter(o->o.priority()!=Priority.LOW).count();
        long assignedHighAndMedium=orderRequest.orders().stream().filter(o->o.priority()!=Priority.LOW&&assignedIds.contains(o.orderId())).count();
        assertEquals(highAndMedium,assignedHighAndMedium,"All HIGH and MEDIUM orders fit in the supplied fleet and should be prioritized");
        assertTrue(plan.totalAssignedLoad()<=500.0+1e-9);
        assertEquals(5,plan.dispatchPlan().size());
        assertTrue(plan.dispatchPlan().stream().allMatch(v->v.currentAddress()!=null&&!v.currentAddress().isBlank()));
    }
}
