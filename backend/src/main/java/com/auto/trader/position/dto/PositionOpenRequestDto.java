package com.auto.trader.position.dto;

import lombok.Data;

@Data
public class PositionOpenRequestDto {
	private Long id; // optional for update
    private Long positionId;
    private double amount;
    private String amountType;
    private double stopLoss;
    private Double takeProfit;
    private String status;
}
