package com.auto.trader.executedreport.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExecutedReportResponseDto {

	private String executedAt;
	private String positionName;
	private String direction;
	private Double executedPrice;
	private Double profitRate;
	private Double observedPrice;
	private Double slippage;
	private Boolean tpSlRegistered;

	private List<ConditionDto> conditions;
	private String executionLog;

	@Getter
	@Builder
	@AllArgsConstructor
	public static class ConditionDto {
		private String indicator;
		private String operator;
		private Double value;
		private String timeframe;
		private String phase;
	}
}