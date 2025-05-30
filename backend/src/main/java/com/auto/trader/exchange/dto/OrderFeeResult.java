package com.auto.trader.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OrderFeeResult {
	private double feeAmount;
	private String feeCurrency;
	private double feeRate; // 수수료율 (예: 0.0004 = 0.04%)
}
