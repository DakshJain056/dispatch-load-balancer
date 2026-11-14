package com.example.dispatch.service;

import com.example.dispatch.entity.DeliveryOrder;
import com.example.dispatch.entity.FleetVehicle;
import org.springframework.stereotype.Component;
import java.util.*;

/** Deterministic capacity-constrained heuristic: best insertion, 2-opt, relocations and cross-route swaps. */
@Component
public class RouteOptimizer {
    private final DistanceCalculator distances;
    public RouteOptimizer(DistanceCalculator distances) { this.distances=distances; }

    public Map<String,List<DeliveryOrder>> allocate(List<FleetVehicle> vehicles, List<DeliveryOrder> orders) {
        Map<String,List<DeliveryOrder>> routes=new LinkedHashMap<>();
        Map<String,Double> loads=new HashMap<>();
        vehicles.stream().sorted(Comparator.comparing(FleetVehicle::getVehicleId)).forEach(v->{routes.put(v.getVehicleId(),new ArrayList<>());loads.put(v.getVehicleId(),0d);});
        List<DeliveryOrder> sorted=new ArrayList<>(orders);
        sorted.sort(Comparator.comparingInt((DeliveryOrder o)->priorityRank(o.getPriority())).thenComparing(DeliveryOrder::getOrderId));
        Map<String,FleetVehicle> byId=new HashMap<>(); vehicles.forEach(v->byId.put(v.getVehicleId(),v));
        for (DeliveryOrder order:sorted) {
            String bestId=null; double bestDelta=Double.POSITIVE_INFINITY; int bestPosition=-1;
            for (FleetVehicle v:vehicles) {
                String id=v.getVehicleId();
                if (loads.get(id)+order.getPackageWeight()>v.getCapacity()+1e-9) continue;
                Insertion insertion=bestInsertion(v,routes.get(id),order);
                if (insertion.delta<bestDelta-1e-9 || (Math.abs(insertion.delta-bestDelta)<=1e-9 && (bestId==null || id.compareTo(bestId)<0))) {
                    bestId=id; bestDelta=insertion.delta; bestPosition=insertion.position;
                }
            }
            if(bestId!=null){routes.get(bestId).add(bestPosition,order);loads.put(bestId,loads.get(bestId)+order.getPackageWeight());}
        }
        for (String id:routes.keySet()) twoOpt(byId.get(id),routes.get(id));
        improveRelocations(routes,byId,loads);
        for (String id:routes.keySet()) twoOpt(byId.get(id),routes.get(id));
        return routes;
    }

    private record Insertion(double delta,int position) {}

    // O(route length) best insertion using only the two affected route edges.
    private Insertion bestInsertion(FleetVehicle v,List<DeliveryOrder> route,DeliveryOrder order){
        double best=Double.POSITIVE_INFINITY;int bestPosition=0;
        for(int p=0;p<=route.size();p++){
            double delta=insertDelta(v,route,order,p);
            if(delta<best){best=delta;bestPosition=p;}
        }
        return new Insertion(best,bestPosition);
    }

    private double insertDelta(FleetVehicle v,List<DeliveryOrder> route,DeliveryOrder order,int position){
        double prevLat=position==0?v.getCurrentLatitude():route.get(position-1).getLatitude();
        double prevLon=position==0?v.getCurrentLongitude():route.get(position-1).getLongitude();
        double delta=distances.haversineKm(prevLat,prevLon,order.getLatitude(),order.getLongitude());
        if(position<route.size()){
            DeliveryOrder next=route.get(position);
            delta+=distances.haversineKm(order.getLatitude(),order.getLongitude(),next.getLatitude(),next.getLongitude());
            delta-=distances.haversineKm(prevLat,prevLon,next.getLatitude(),next.getLongitude());
        }
        return delta;
    }

    // The symmetric Haversine metric means internal reversed edges keep the same cost.
    private void twoOpt(FleetVehicle v,List<DeliveryOrder> route){
        boolean changed=true;int passes=0;
        while(changed&&passes++<10){changed=false;double bestSaving=1e-8;int bestI=-1,bestJ=-1;
            for(int i=0;i<route.size()-1;i++)for(int j=i+1;j<route.size();j++){
                double saving=twoOptSaving(v,route,i,j);
                if(saving>bestSaving){bestSaving=saving;bestI=i;bestJ=j;}
            }
            if(bestI>=0){Collections.reverse(route.subList(bestI,bestJ+1));changed=true;}
        }
    }
    private double twoOptSaving(FleetVehicle v,List<DeliveryOrder> r,int i,int j){
        double prevLat=i==0?v.getCurrentLatitude():r.get(i-1).getLatitude();
        double prevLon=i==0?v.getCurrentLongitude():r.get(i-1).getLongitude();
        DeliveryOrder first=r.get(i),last=r.get(j);
        double before=distances.haversineKm(prevLat,prevLon,first.getLatitude(),first.getLongitude());
        double after=distances.haversineKm(prevLat,prevLon,last.getLatitude(),last.getLongitude());
        if(j+1<r.size()){
            DeliveryOrder next=r.get(j+1);
            before+=distances.haversineKm(last.getLatitude(),last.getLongitude(),next.getLatitude(),next.getLongitude());
            after+=distances.haversineKm(first.getLatitude(),first.getLongitude(),next.getLatitude(),next.getLongitude());
        }
        return before-after;
    }

    private void improveRelocations(Map<String,List<DeliveryOrder>> routes,Map<String,FleetVehicle> vehicles,Map<String,Double> loads){
        List<String> ids=new ArrayList<>(routes.keySet());
        // Bounded best-improvement passes; edge deltas avoid recalculating entire routes per candidate.
        for(int pass=0;pass<25;pass++){
            double bestSaving=1e-8;String fromBest=null,toBest=null;int indexBest=-1,posBest=-1;
            for(String from:ids)for(String to:ids){if(from.equals(to))continue;
                List<DeliveryOrder> source=routes.get(from),target=routes.get(to);
                for(int i=0;i<source.size();i++){
                    DeliveryOrder order=source.get(i);
                    if(loads.get(to)+order.getPackageWeight()>vehicles.get(to).getCapacity()+1e-9)continue;
                    double removal=removeDelta(vehicles.get(from),source,i);
                    for(int p=0;p<=target.size();p++){
                        double saving=-(removal+insertDelta(vehicles.get(to),target,order,p));
                        if(saving>bestSaving){bestSaving=saving;fromBest=from;toBest=to;indexBest=i;posBest=p;}
                    }
                }
            }
            if(fromBest==null)break;
            DeliveryOrder moved=routes.get(fromBest).remove(indexBest);routes.get(toBest).add(posBest,moved);
            loads.put(fromBest,loads.get(fromBest)-moved.getPackageWeight());loads.put(toBest,loads.get(toBest)+moved.getPackageWeight());
        }
        // Capacity-safe cross-route swaps can improve clustering when a one-way move is not feasible.
        for(int pass=0;pass<15;pass++){
            double bestSaving=1e-8;String bestA=null,bestB=null;int bestI=-1,bestJ=-1;
            for(int a=0;a<ids.size();a++)for(int b=a+1;b<ids.size();b++){
                String idA=ids.get(a),idB=ids.get(b);List<DeliveryOrder> ra=routes.get(idA),rb=routes.get(idB);
                for(int i=0;i<ra.size();i++)for(int j=0;j<rb.size();j++){
                    DeliveryOrder oa=ra.get(i),ob=rb.get(j);
                    double newA=loads.get(idA)-oa.getPackageWeight()+ob.getPackageWeight();
                    double newB=loads.get(idB)-ob.getPackageWeight()+oa.getPackageWeight();
                    if(newA>vehicles.get(idA).getCapacity()+1e-9||newB>vehicles.get(idB).getCapacity()+1e-9)continue;
                    double saving=-(replaceDelta(vehicles.get(idA),ra,i,ob)+replaceDelta(vehicles.get(idB),rb,j,oa));
                    if(saving>bestSaving){bestSaving=saving;bestA=idA;bestB=idB;bestI=i;bestJ=j;}
                }
            }
            if(bestA==null)break;
            List<DeliveryOrder> ra=routes.get(bestA),rb=routes.get(bestB);DeliveryOrder oa=ra.get(bestI),ob=rb.get(bestJ);
            ra.set(bestI,ob);rb.set(bestJ,oa);
            loads.put(bestA,loads.get(bestA)-oa.getPackageWeight()+ob.getPackageWeight());
            loads.put(bestB,loads.get(bestB)-ob.getPackageWeight()+oa.getPackageWeight());
        }
    }

    private double removeDelta(FleetVehicle v,List<DeliveryOrder> route,int index){
        DeliveryOrder current=route.get(index);
        double prevLat=index==0?v.getCurrentLatitude():route.get(index-1).getLatitude();
        double prevLon=index==0?v.getCurrentLongitude():route.get(index-1).getLongitude();
        double delta=-distances.haversineKm(prevLat,prevLon,current.getLatitude(),current.getLongitude());
        if(index+1<route.size()){
            DeliveryOrder next=route.get(index+1);
            delta-=distances.haversineKm(current.getLatitude(),current.getLongitude(),next.getLatitude(),next.getLongitude());
            delta+=distances.haversineKm(prevLat,prevLon,next.getLatitude(),next.getLongitude());
        }
        return delta;
    }

    private double replaceDelta(FleetVehicle v,List<DeliveryOrder> route,int index,DeliveryOrder replacement){
        DeliveryOrder old=route.get(index);
        double prevLat=index==0?v.getCurrentLatitude():route.get(index-1).getLatitude();
        double prevLon=index==0?v.getCurrentLongitude():route.get(index-1).getLongitude();
        double delta=distances.haversineKm(prevLat,prevLon,replacement.getLatitude(),replacement.getLongitude())
                -distances.haversineKm(prevLat,prevLon,old.getLatitude(),old.getLongitude());
        if(index+1<route.size()){
            DeliveryOrder next=route.get(index+1);
            delta+=distances.haversineKm(replacement.getLatitude(),replacement.getLongitude(),next.getLatitude(),next.getLongitude())
                    -distances.haversineKm(old.getLatitude(),old.getLongitude(),next.getLatitude(),next.getLongitude());
        }
        return delta;
    }

    private int priorityRank(String p){return switch(p){case "HIGH"->0;case "MEDIUM"->1;default->2;};}
}
