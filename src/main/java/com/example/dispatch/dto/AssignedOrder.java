package com.example.dispatch.dto;
public record AssignedOrder(String orderId, double latitude, double longitude, String address, double packageWeight, String priority) {}
