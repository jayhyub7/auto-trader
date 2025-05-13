package com.auto.trader.trade.indicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.auto.trader.trade.dto.CandleDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandleAggregator {

  private final IndicatorCalculator indicatorCalculator;

  private static final List<String> TIMEFRAMES = List.of("1m", "3m", "5m", "15m", "1h", "4h", "1d");
  private static final Map<String, Long> INTERVAL_MILLIS =
      Map.of("1m", 60_000L, "3m", 3 * 60_000L, "5m", 5 * 60_000L, "15m", 15 * 60_000L, "1h",
          60 * 60_000L, "4h", 4 * 60 * 60_000L, "1d", 24 * 60 * 60_000L);

  private final Map<String, List<CandleDto>> candleMap = new HashMap<>();
  private final Map<String, CandleDto> currentCandleMap = new HashMap<>();
  private final RestTemplate restTemplate = new RestTemplate();

  @PostConstruct
  public void init() {
    for (String tf : TIMEFRAMES) {
      List<CandleDto> candles = loadInitialCandles("BTCUSDT", tf);
      candleMap.put(tf, candles);
      currentCandleMap.put(tf, null); // 초기 캔들은 틱 수신 시 생성
    }
  }

  public synchronized void handleTick(String symbol, double price, long timestamp) {
    for (String tf : TIMEFRAMES) {
      long interval = INTERVAL_MILLIS.get(tf);
      long bucketTime = timestamp - (timestamp % interval);

      List<CandleDto> candles = candleMap.get(tf);
      CandleDto current = currentCandleMap.get(tf);

      double roundedPrice = roundToDecimals(price, 4);

      if (current == null || current.getTime() != bucketTime) {
        if (current != null) {
          candles.add(current);
        }

        CandleDto newCandle = new CandleDto();
        newCandle.setTime(bucketTime);
        newCandle.setOpen(roundedPrice);
        newCandle.setHigh(roundedPrice);
        newCandle.setLow(roundedPrice);
        newCandle.setClose(roundedPrice);
        newCandle.setVolume(1);
        currentCandleMap.put(tf, newCandle);

        List<CandleDto> fullList = new ArrayList<>(candles);
        fullList.add(newCandle);
        indicatorCalculator.calculateAndStore(symbol, tf, fullList);
      } else {
        current.setClose(roundedPrice);
        current.setHigh(Math.max(current.getHigh(), roundedPrice));
        current.setLow(Math.min(current.getLow(), roundedPrice));
        current.setVolume(current.getVolume() + 1);

        List<CandleDto> fullList = new ArrayList<>(candles);
        fullList.add(current);
        indicatorCalculator.calculateAndStore(symbol, tf, fullList);
      }
    }
  }

  private double roundToDecimals(double value, int decimals) {
    double scale = Math.pow(10, decimals);
    return Math.round(value * scale) / scale;
  }

  public List<CandleDto> getCandles(String timeframe) {
    return candleMap.getOrDefault(timeframe, List.of());
  }


  private List<CandleDto> loadInitialCandles(String symbol, String timeframe) {
    try {
      String url =
          String.format("https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=500",
              symbol, timeframe);
      Object[][] response = restTemplate.getForObject(url, Object[][].class);
      List<CandleDto> result = new ArrayList<>();
      if (response != null) {
        for (Object[] o : response) {
          CandleDto c = new CandleDto();
          c.setTime(((Number) o[0]).longValue());
          c.setOpen(roundToDecimals(Double.parseDouble((String) o[1]), 4));
          c.setHigh(roundToDecimals(Double.parseDouble((String) o[2]), 4));
          c.setLow(roundToDecimals(Double.parseDouble((String) o[3]), 4));
          c.setClose(roundToDecimals(Double.parseDouble((String) o[4]), 4));
          c.setVolume(roundToDecimals(Double.parseDouble((String) o[5]), 2)); // volume은 소수점 2자리
                                                                              // 정도로도 충분
          result.add(c);
        }
      }
      log.info("📥 초기 캔들 불러오기 완료 [{}]: {}", timeframe, result.size());
      return result;
    } catch (Exception e) {
      log.error("❌ 초기 캔들 로드 실패 [{}]", timeframe, e);
      return new ArrayList<>();
    }
  }
}
