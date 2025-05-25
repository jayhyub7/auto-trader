package com.auto.trader.position.dto;

import lombok.Data;

@Data
public class PositionOpenRequestDto {
	private Long id; // optional for update
	private Long positionId;
	private int leverage;
	private double amount;
	private String amountType;
	private Double simulatedAvailable;
	private double stopLoss;
	private Double takeProfit;
	private String status;
	private boolean simulating; // ✅ Position에 반영되는 시뮬레이션 여부
}
