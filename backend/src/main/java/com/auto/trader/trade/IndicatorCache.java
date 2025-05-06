package com.auto.trader.trade;

import com.auto.trader.trade.dto.CandleDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class IndicatorCache {

    private final List<CandleDto> candles; // 최대 500개
    private final List<IndicatorUtil.IndicatorPoint> rsi;
    private final List<IndicatorUtil.DualIndicatorPoint> stochRsi;
    private final IndicatorUtil.VWBB vwbb;

}
