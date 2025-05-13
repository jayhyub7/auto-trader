// 📁 com.auto.trader.trade.indicator.IndicatorCalculator.java

package com.auto.trader.trade.indicator;

import java.util.Comparator;
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
        return;

      candles.sort(Comparator.comparingLong(CandleDto::getTime));

      // 실전 기준: 마지막 캔들 제외하고 계산
      List<CandleDto> trimmed = candles.subList(0, candles.size() - 1);

      List<IndicatorUtil.IndicatorPoint> rsi = IndicatorUtil.calculateRSI(trimmed, 14);
      List<IndicatorUtil.DualIndicatorPoint> stoch =
          IndicatorUtil.calculateStochRSI(trimmed, 14, 14, 3, 3); // ✅ 실전 기준

      System.out.println("🧪 VWBB 시작 시점: 첫 캔들 time = " + candles.get(0).getTime());
      System.out
          .println("🧪 VWBB 마지막 시점: 마지막 캔들 time = " + candles.get(candles.size() - 1).getTime());
      System.out.println("🧪 VWBB 총 캔들 수: " + candles.size(

      ));
      IndicatorUtil.VWBB vwbb = IndicatorUtil.calculateVWBB(trimmed, 20, 2);

      double currentPrice = candles.get(candles.size() - 1).getClose(); // ✅ 현재가

      IndicatorCache cache = new IndicatorCache(candles, rsi, stoch, vwbb, currentPrice);
      String key = symbol + "_" + timeframe;
      IndicatorMemoryStore.put(key, cache);

    } catch (Exception e) {
      log.error("❌ 지표 계산 실패 [{}_{}]", symbol, timeframe, e);
    }
  }
}
