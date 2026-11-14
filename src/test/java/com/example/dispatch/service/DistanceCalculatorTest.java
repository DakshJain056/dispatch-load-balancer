package com.example.dispatch.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DistanceCalculatorTest {
    private final DistanceCalculator calculator = new DistanceCalculator();

    @Test void samePointHasZeroDistance() {
        assertEquals(0.0, calculator.haversineKm(28.6139, 77.2090, 28.6139, 77.2090), 1e-9);
    }

    @Test void calculatesDelhiToNoidaDistanceApproximately() {
        double distance = calculator.haversineKm(28.6139, 77.2090, 28.5355, 77.3910);
        assertTrue(distance > 18 && distance < 22, "Expected approximately 20 km, got " + distance);
    }

    @Test void distanceIsSymmetric() {
        double a = calculator.haversineKm(12.9716, 77.5946, 13.0827, 80.2707);
        double b = calculator.haversineKm(13.0827, 80.2707, 12.9716, 77.5946);
        assertEquals(a, b, 1e-9);
    }
}
