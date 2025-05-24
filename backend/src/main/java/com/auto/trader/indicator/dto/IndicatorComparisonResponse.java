// IndicatorComparisonResponse.java

package com.auto.trader.indicator.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndicatorComparisonResponse {
	private Map<String, List<Map<String, Object>>> result;
}