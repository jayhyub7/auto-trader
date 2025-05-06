package com.auto.trader.trade;

import com.auto.trader.trade.dto.CandleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandleAggregator {

    private final IndicatorCalculator indicatorCalculator;

    private static final List<String> TIMEFRAMES = List.of("1m", "3m", "5m", "15m", "1h", "4h", "1d");
    private static final Map<String, Long> INTERVAL_MILLIS = Map.of(
        "1m", 60_000L,
        "3m", 3 * 60_000L,
        "5m", 5 * 60_000L,
        "15m", 15 * 60_000L,
        "1h", 60 * 60_000L,
        "4h", 4 * 60 * 60_000L,
        "1d", 24 * 60 * 60_000L
    );

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

            if (current == null || current.getTime() != bucketTime) {
                // 이전 캔들을 리스트에 추가
                if (current != null) {
                    candles.add(current);
                    if (candles.size() > 500) candles.remove(0);
                    indicatorCalculator.calculateAndStore(symbol, tf, new ArrayList<>(candles));
                }

                // 새 캔들 시작
                CandleDto newCandle = new CandleDto();
                newCandle.setTime(bucketTime);
                newCandle.setOpen(price);
                newCandle.setHigh(price);
                newCandle.setLow(price);
                newCandle.setClose(price);
                newCandle.setVolume(1);
                currentCandleMap.put(tf, newCandle);
            } else {
                // 기존 캔들 업데이트
                current.setClose(price);
                current.setHigh(Math.max(current.getHigh(), price));
                current.setLow(Math.min(current.getLow(), price));
                current.setVolume(current.getVolume() + 1);
            }
        }
    }

    private List<CandleDto> loadInitialCandles(String symbol, String timeframe) {
        try {
            String url = String.format("https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=500", symbol, timeframe);
            Object[][] response = restTemplate.getForObject(url, Object[][].class);
            List<CandleDto> result = new ArrayList<>();
            if (response != null) {
                for (Object[] o : response) {
                    CandleDto c = new CandleDto();
                    c.setTime(((Number) o[0]).longValue());
                    c.setOpen(Double.parseDouble((String) o[1]));
                    c.setHigh(Double.parseDouble((String) o[2]));
                    c.setLow(Double.parseDouble((String) o[3]));
                    c.setClose(Double.parseDouble((String) o[4]));
                    c.setVolume(Double.parseDouble((String) o[5]));
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
