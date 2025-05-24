package com.auto.trader.indicator.dto;

import java.util.List;

import com.auto.trader.trade.dto.CandleDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndicatorComparisonRequest {
	private String symbol;
	private String interval;
	private List<CandleDto> frontendCandles; // ✅ 프론트 비교에만 사용
}
