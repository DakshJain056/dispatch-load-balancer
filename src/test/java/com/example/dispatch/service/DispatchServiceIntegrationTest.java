package com.example.dispatch.service;

import com.example.dispatch.dto.*;
import com.example.dispatch.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchServiceIntegrationTest {
    @Autowired DispatchService service;
    @Autowired DeliveryOrderRepository orders;
    @Autowired FleetVehicleRepository vehicles;
    @Autowired MockMvc mockMvc;
    @BeforeEach void clean(){service.clearAll();}
    @AfterEach void cleanAfter(){service.clearAll();}

    @Test void upsertUpdatesExistingOrderAndVehicle(){
        service.upsertVehicles(List.of(new VehicleRequest("V1",100d,28.6,77.2,"Depot")));
        service.upsertOrders(List.of(new OrderRequest("O1",28.61,77.21,"Old",10d,Priority.HIGH)));
        service.upsertOrders(List.of(new OrderRequest("O1",28.62,77.22,"New",20d,Priority.MEDIUM)));
        service.upsertVehicles(List.of(new VehicleRequest("V1",120d,28.7,77.3,"New depot")));
        assertEquals(1,orders.count());assertEquals(1,vehicles.count());
        var plan=service.buildPlan();assertEquals(20.0,plan.totalAssignedLoad(),0.001);
        assertEquals(120.0,plan.dispatchPlan().get(0).capacity(),0.001);
        assertEquals("New depot",plan.dispatchPlan().get(0).currentAddress());
        assertEquals("New",plan.dispatchPlan().get(0).assignedOrders().get(0).address());
    }

    @Test void duplicateIdsInSameRequestAreRejected(){
        var duplicate=new OrderRequest("DUP",1d,1d,"A",1d,Priority.HIGH);
        assertThrows(IllegalArgumentException.class,()->service.upsertOrders(List.of(duplicate,duplicate)));
    }

    @Test void invalidCoordinatesAndWeightsReturnHttp400() throws Exception {
        String json="{\"orders\":[{\"orderId\":\"BAD\",\"latitude\":120,\"longitude\":77,\"address\":\"Bad\",\"packageWeight\":-1,\"priority\":\"HIGH\"}]}";
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test void noVehicleCaseReturnsAllOrdersAsUnassigned(){
        service.upsertOrders(List.of(new OrderRequest("O1",1d,1d,"Drop",1d,Priority.HIGH)));
        var plan=service.buildPlan();assertEquals(0,plan.assignedOrderCount());
        assertEquals("No vehicles have been registered",plan.unassignedOrders().get(0).reason());
    }

    @Test void unassignableOrderReturnsUsefulReason(){
        service.upsertVehicles(List.of(new VehicleRequest("V1",5d,0d,0d,"Depot")));
        service.upsertOrders(List.of(new OrderRequest("O1",1d,1d,"Drop",10d,Priority.HIGH)));
        var plan=service.buildPlan();assertEquals(0,plan.assignedOrderCount());assertEquals(1,plan.unassignedOrderCount());
        assertTrue(plan.unassignedOrders().get(0).reason().contains("exceeds"));
    }
}
