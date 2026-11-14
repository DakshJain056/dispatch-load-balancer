package com.example.dispatch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fleet_vehicles")
public class FleetVehicle {
    @Id @Column(length = 64) private String vehicleId;
    private double capacity;
    private double currentLatitude;
    private double currentLongitude;
    @Column(nullable = false, length = 500) private String currentAddress;
    protected FleetVehicle() {}
    public FleetVehicle(String vehicleId, double capacity, double currentLatitude, double currentLongitude, String currentAddress) {
        this.vehicleId=vehicleId; this.capacity=capacity; this.currentLatitude=currentLatitude; this.currentLongitude=currentLongitude; this.currentAddress=currentAddress;
    }
    public String getVehicleId(){return vehicleId;} public double getCapacity(){return capacity;}
    public double getCurrentLatitude(){return currentLatitude;} public double getCurrentLongitude(){return currentLongitude;}
    public String getCurrentAddress(){return currentAddress;}
}
