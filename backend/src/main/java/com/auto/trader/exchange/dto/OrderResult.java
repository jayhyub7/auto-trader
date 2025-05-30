package com.auto.trader.exchange.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderResult {
	private boolean success;
	private boolean tpSlSuccess = false; // 기본값
	private String orderId;
	private String symbol;
	private double executedQty;
	private double price;
	private String rawResponse;
	private double executionTimeSeconds = 0.0; // 기본값
	private double feeAmount;
	private String feeCurrency;
	private double feeRate; // 수수료율 (예: 0.0004 = 0.04%)

	public OrderResult(boolean success, String orderId, String symbol, double executedQty, double price,
			String rawResponse) {
		this.success = success;
		this.orderId = orderId;
		this.symbol = symbol;
		this.executedQty = executedQty;
		this.price = price;
		this.rawResponse = rawResponse;
	}
}
