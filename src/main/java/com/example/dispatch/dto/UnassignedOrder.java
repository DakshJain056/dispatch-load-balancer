package com.example.dispatch.dto;
public record UnassignedOrder(String orderId, String priority, double packageWeight, String reason) {}
