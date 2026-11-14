package com.example.dispatch.service;

import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {
    public static final double EARTH_RADIUS_KM = 6371.0088;
    public double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double p1=Math.toRadians(lat1), p2=Math.toRadians(lat2);
        double dLat=Math.toRadians(lat2-lat1), dLon=Math.toRadians(lon2-lon1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2*EARTH_RADIUS_KM*Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0,1-a)));
    }
}
