package com.example.dispatch.service;

import com.example.dispatch.dto.*;
import com.example.dispatch.entity.*;
import com.example.dispatch.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchService {
    private final DeliveryOrderRepository orderRepository;
    private final FleetVehicleRepository vehicleRepository;
    private final RouteOptimizer optimizer;
    private final DistanceCalculator distances;

    public DispatchService(DeliveryOrderRepository orderRepository, FleetVehicleRepository vehicleRepository,
                           RouteOptimizer optimizer, DistanceCalculator distances) {
        this.orderRepository=orderRepository; this.vehicleRepository=vehicleRepository; this.optimizer=optimizer; this.distances=distances;
    }

    @Transactional
    public int upsertOrders(List<OrderRequest> incoming) {
        rejectDuplicateIds(incoming.stream().map(OrderRequest::orderId).toList(), "orderId");
        List<DeliveryOrder> entities=incoming.stream().map(o->new DeliveryOrder(o.orderId().trim(),o.latitude(),o.longitude(),
                o.address().trim(),o.packageWeight(),o.priority().name())).toList();
        orderRepository.saveAll(entities); orderRepository.flush(); return entities.size();
    }

    @Transactional
    public int upsertVehicles(List<VehicleRequest> incoming) {
        rejectDuplicateIds(incoming.stream().map(VehicleRequest::vehicleId).toList(), "vehicleId");
        List<FleetVehicle> entities=incoming.stream().map(v->new FleetVehicle(v.vehicleId().trim(),v.capacity(),v.currentLatitude(),
                v.currentLongitude(),v.currentAddress().trim())).toList();
        vehicleRepository.saveAll(entities); vehicleRepository.flush(); return entities.size();
    }

    @Transactional(readOnly=true)
    public DispatchPlanResponse buildPlan() {
        List<DeliveryOrder> orders=orderRepository.findAll();
        List<FleetVehicle> vehicles=vehicleRepository.findAll().stream().sorted(Comparator.comparing(FleetVehicle::getVehicleId)).toList();
        Map<String,List<DeliveryOrder>> routes=optimizer.allocate(vehicles,orders);
        Set<String> assignedIds=routes.values().stream().flatMap(Collection::stream).map(DeliveryOrder::getOrderId).collect(Collectors.toSet());
        List<UnassignedOrder> unassigned=orders.stream().filter(o->!assignedIds.contains(o.getOrderId()))
                .sorted(Comparator.comparingInt((DeliveryOrder o)->priorityRank(o.getPriority())).thenComparing(DeliveryOrder::getOrderId))
                .map(o->new UnassignedOrder(o.getOrderId(),o.getPriority(),round(o.getPackageWeight()),reason(o,vehicles))).toList();
        List<VehicleDispatch> plan=new ArrayList<>(); double totalLoad=0,totalDistance=0;
        for(FleetVehicle v:vehicles){List<DeliveryOrder> route=routes.getOrDefault(v.getVehicleId(),List.of());
            double load=route.stream().mapToDouble(DeliveryOrder::getPackageWeight).sum();double km=routeCost(v,route);
            totalLoad+=load;totalDistance+=km;
            List<AssignedOrder> assigned=route.stream().map(o->new AssignedOrder(o.getOrderId(),o.getLatitude(),o.getLongitude(),o.getAddress(),
                    round(o.getPackageWeight()),o.getPriority())).toList();
            plan.add(new VehicleDispatch(v.getVehicleId(),round(v.getCapacity()),v.getCurrentLatitude(),v.getCurrentLongitude(),v.getCurrentAddress(),
                    round(load),String.format(Locale.ROOT,"%.2f km",km),round(km),assigned));
        }
        return new DispatchPlanResponse(List.copyOf(plan),unassigned,round(totalLoad),round(totalDistance),assignedIds.size(),unassigned.size(),
                "Priority-first best-insertion allocation + 2-opt + capacity-safe inter-vehicle relocation and swap refinement",
                "One-way route: vehicle current location through assigned stops; no return-to-depot leg");
    }

    @Transactional
    public void clearAll(){orderRepository.deleteAllInBatch();vehicleRepository.deleteAllInBatch();}

    private double routeCost(FleetVehicle v,List<DeliveryOrder> route){double km=0,lat=v.getCurrentLatitude(),lon=v.getCurrentLongitude();
        for(DeliveryOrder o:route){km+=distances.haversineKm(lat,lon,o.getLatitude(),o.getLongitude());lat=o.getLatitude();lon=o.getLongitude();}return km;}
    private String reason(DeliveryOrder order,List<FleetVehicle> vehicles){
        if(vehicles.isEmpty())return "No vehicles have been registered";
        if(vehicles.stream().noneMatch(v->v.getCapacity()+1e-9>=order.getPackageWeight()))return "Order weight exceeds every vehicle's capacity";
        return "Insufficient remaining fleet capacity after priority-ordered allocations";
    }
    private int priorityRank(String p){return switch(p){case "HIGH"->0;case "MEDIUM"->1;default->2;};}
    private double round(double n){return Math.round(n*100.0)/100.0;}
    private void rejectDuplicateIds(List<String> ids,String field){Set<String> seen=new HashSet<>();for(String id:ids)if(!seen.add(id.trim()))throw new IllegalArgumentException("Duplicate "+field+" in request: "+id);}
}
