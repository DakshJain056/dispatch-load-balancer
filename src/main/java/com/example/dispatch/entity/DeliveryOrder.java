package com.example.dispatch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrder {
    @Id @Column(length = 64) private String orderId;
    private double latitude;
    private double longitude;
    @Column(nullable = false, length = 500) private String address;
    private double packageWeight;
    @Column(nullable = false, length = 10) private String priority;
    protected DeliveryOrder() {}
    public DeliveryOrder(String orderId, double latitude, double longitude, String address, double packageWeight, String priority) {
        this.orderId=orderId; this.latitude=latitude; this.longitude=longitude; this.address=address; this.packageWeight=packageWeight; this.priority=priority;
    }
    public String getOrderId(){return orderId;} public double getLatitude(){return latitude;} public double getLongitude(){return longitude;}
    public String getAddress(){return address;} public double getPackageWeight(){return packageWeight;} public String getPriority(){return priority;}
}
