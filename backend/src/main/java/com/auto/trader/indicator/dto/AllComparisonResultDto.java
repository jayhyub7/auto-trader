package com.auto.trader.indicator.dto;

import java.util.List;

import com.auto.trader.trade.dto.CandleDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllComparisonResultDto {
	private List<ComparisonPoint> rsi;
	private List<ComparisonPoint> sma;
	private List<ComparisonPoint> ema;
	private VWBBComparison vwbb;
	private List<StochRsiComparisonPoint> stochRsi;
	private List<CandleComparison> candles;

	@Data
	@AllArgsConstructor
	public static class ComparisonPoint {
		private long time;
		private Double frontend;
		private Double backend;
		private Double diff;
	}

	@Data
	@AllArgsConstructor
	public static class VWBBComparison {
		private List<ComparisonPoint> upper;
		private List<ComparisonPoint> lower;
		private List<ComparisonPoint> basis;
	}

	@Data
	@AllArgsConstructor
	public static class StochRsiComparisonPoint {
		private long time;
		private Double kFrontend, kBackend, kDiff;
		private Double dFrontend, dBackend, dDiff;
	}

	@Data
	@AllArgsConstructor
	public static class CandleComparison {
		private long time;
		private CandleDto frontend;
		private CandleDto backend;
		private CandleDiff diff; // optional
	}

	@Data
	@AllArgsConstructor
	public static class CandleDiff {
		private Double close;
		private Double volume;
	}
}
