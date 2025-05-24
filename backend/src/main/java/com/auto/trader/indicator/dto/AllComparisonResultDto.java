package com.auto.trader.indicator.dto;

import java.util.List;

import com.auto.trader.trade.indicator.IndicatorUtil.DualIndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.IndicatorPoint;
import com.auto.trader.trade.indicator.IndicatorUtil.VWBB;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllComparisonResultDto {
	List<IndicatorPoint> rsi;
	List<DualIndicatorPoint> stochRSI;
	VWBB vwbb;
}
