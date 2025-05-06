package com.auto.trader.position.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionOpenDto {
    private Long id;
    private Long positionId;
    private String status;
    private String amountType;
    private double amount;
    private double stopLoss;
    private Double takeProfit;
}