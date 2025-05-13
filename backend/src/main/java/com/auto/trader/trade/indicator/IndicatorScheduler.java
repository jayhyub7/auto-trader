// 📁 com.auto.trader.trade.indicator.IndicatorScheduler.java

package com.auto.trader.trade.indicator;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.auto.trader.trade.dto.CandleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorScheduler {

  private final CandleAggregator candleAggregator;
  private final IndicatorCalculator indicatorCalculator;

  // VWBB, RSI, StochRSI 계산 후 메모리에 저장
  @Scheduled(fixedDelay = 1000)
  public void updateIndicators() {
    try {
      String SYMBOL = "BTCUSDT";
      List<String> timeframes = List.of("1m", "3m", "5m", "15m", "1h", "4h");

      for (String timeframe : timeframes) {
        List<CandleDto> candles = candleAggregator.getCandles(timeframe);
        if (candles == null || candles.size() < 50) {
          log.warn("⚠️ 캔들 부족 또는 없음: {}", timeframe);
          continue;
        }

        indicatorCalculator.calculateAndStore(SYMBOL, timeframe, candles);
      }
    } catch (Exception e) {
      log.error("📉 지표 업데이트 실패", e);
    }
  }
}
