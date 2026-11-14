package com.example.dispatch.service;

import com.example.dispatch.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizerTest {
    private final RouteOptimizer optimizer=new RouteOptimizer(new DistanceCalculator());
    @Test void neverExceedsVehicleCapacityAndAllocatesHighBeforeLow(){
        FleetVehicle v=new FleetVehicle("V1",50,28.6,77.2,"Depot");
        List<DeliveryOrder> orders=List.of(
            new DeliveryOrder("LOW",28.61,77.21,"Low",10,"LOW"),
            new DeliveryOrder("HIGH",28.62,77.22,"High",40,"HIGH"),
            new DeliveryOrder("MED",28.63,77.23,"Med",10,"MEDIUM"));
        Map<String,List<DeliveryOrder>> routes=optimizer.allocate(List.of(v),orders);
        List<DeliveryOrder> assigned=routes.get("V1");
        assertEquals(2,assigned.size());assertTrue(assigned.stream().anyMatch(o->o.getOrderId().equals("HIGH")));assertTrue(assigned.stream().anyMatch(o->o.getOrderId().equals("MED")));assertFalse(assigned.stream().anyMatch(o->o.getOrderId().equals("LOW")));
        assertTrue(assigned.stream().mapToDouble(DeliveryOrder::getPackageWeight).sum()<=v.getCapacity());
    }
    @Test void distributesOrdersAcrossVehiclesWhenNeeded(){
        FleetVehicle a=new FleetVehicle("A",10,28.6,77.2,"A");FleetVehicle b=new FleetVehicle("B",10,28.7,77.3,"B");
        List<DeliveryOrder> orders=List.of(new DeliveryOrder("O1",28.61,77.21,"1",10,"HIGH"),new DeliveryOrder("O2",28.71,77.31,"2",10,"HIGH"));
        Map<String,List<DeliveryOrder>> result=optimizer.allocate(List.of(a,b),orders);
        assertEquals(2,result.values().stream().mapToInt(List::size).sum());
        assertTrue(result.values().stream().allMatch(r->r.size()<=1));
    }
    @Test void emptyFleetProducesNoRoutes(){
        Map<String,List<DeliveryOrder>> result=optimizer.allocate(List.of(),List.of(new DeliveryOrder("O",0,0,"x",1,"HIGH")));
        assertTrue(result.isEmpty());
    }
}
