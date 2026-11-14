package com.example.dispatch.repository;
import com.example.dispatch.entity.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, String> {}
