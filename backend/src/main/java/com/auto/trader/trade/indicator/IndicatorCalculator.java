package com.auto.trader.trade.indicator;

import java.util.List;
import org.springframework.stereotype.Component;
import com.auto.trader.trade.dto.CandleDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IndicatorCalculator {

  public void calculateAndStore(String symbol, String timeframe, List<CandleDto> candles) {
    try {
      if (candles.size() < 50)
        return; // 안정성 확보

      List<IndicatorUtil.IndicatorPoint> rsi = IndicatorUtil.calculateRSI(candles, 14);
      List<IndicatorUtil.DualIndicatorPoint> stoch =
          IndicatorUtil.calculateStochRSI(candles, 14, 3);
      // List<CandleDto> trimmedCandles = candles.subList(0, candles.size() - 1);
      IndicatorUtil.VWBB vwbb = IndicatorUtil.calculateVWBB(candles, 20, 2);

      double currentPrice = candles.get(candles.size() - 1).getClose(); // ✅ 현재 가격

      IndicatorCache cache = new IndicatorCache(candles, rsi, stoch, vwbb, currentPrice); // ✅ 가격 포함
      String key = symbol + "_" + timeframe;
      IndicatorMemoryStore.put(key, cache);

    } catch (Exception e) {
      log.error("❌ 지표 계산 실패 [{}_{}]", symbol, timeframe, e);
    }
  }

}
