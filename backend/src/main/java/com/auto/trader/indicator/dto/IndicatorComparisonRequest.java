package com.auto.trader.indicator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorComparisonRequest {
	private String symbol;
	private String interval;
}
