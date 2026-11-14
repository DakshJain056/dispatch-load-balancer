package com.example.dispatch.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DistanceCalculatorTest {
    private final DistanceCalculator calculator=new DistanceCalculator();
    @Test void samePointHasZeroDistance(){assertEquals(0,calculator.haversineKm(28.6,77.2,28.6,77.2),1e-9);}
    @Test void distanceIsSymmetric(){double a=calculator.haversineKm(28.6139,77.209,28.5355,77.391);double b=calculator.haversineKm(28.5355,77.391,28.6139,77.209);assertEquals(a,b,1e-9);}
    @Test void knownDelhiNoidaDistanceIsReasonable(){double km=calculator.haversineKm(28.6139,77.209,28.5355,77.391);assertTrue(km>18&&km<22,"Expected roughly 20 km, got "+km);}
}
