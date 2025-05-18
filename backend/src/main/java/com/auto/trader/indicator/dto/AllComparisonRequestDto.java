package com.auto.trader.indicator.dto;

import java.util.List;

import com.auto.trader.trade.dto.CandleDto;

import lombok.Data;

@Data
public class AllComparisonRequestDto {
	private String symbol;
	private String timeframe;
	private List<CandleDto> candles;
}
