package com.auto.trader.trade;

import com.auto.trader.trade.dto.CandleDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorScheduler {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String SYMBOL = "BTCUSDT";
    private static final List<String> TIMEFRAMES = List.of("1m", "3m", "5m", "15m", "1h", "4h", "1d");
    private static final String BINANCE_API = "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d";

    private final Map<String, List<CandleDto>> candleCache = new HashMap<>();
    private final Set<String> initialized = new HashSet<>();

    @PostConstruct
    public void init() {
        TIMEFRAMES.forEach(tf -> candleCache.put(tf, new ArrayList<>()));
    }

    @Scheduled(fixedDelay = 1000)
    public void updateIndicators() {
        for (String timeframe : TIMEFRAMES) {
            try {
                List<CandleDto> candles = candleCache.get(timeframe);
                if (!initialized.contains(timeframe)) {
                    // 최초 1회만 500개 로드
                    String url = String.format(BINANCE_API, SYMBOL, timeframe, 500);
                    Object[][] response = restTemplate.getForObject(url, Object[][].class);
                    if (response == null || response.length == 0) continue;

                    List<CandleDto> initial = toCandleList(response);
                    candles.clear();
                    candles.addAll(initial);
                    initialized.add(timeframe);
                } else {
                    // 이후에는 최신 2개만 요청해서 업데이트
                    String url = String.format(BINANCE_API, SYMBOL, timeframe, 2);
                    Object[][] response = restTemplate.getForObject(url, Object[][].class);
                    if (response == null || response.length < 1) continue;

                    List<CandleDto> latest = toCandleList(response);
                    CandleDto newCandle = latest.get(latest.size() - 1);
                    long lastTime = candles.get(candles.size() - 1).getTime();

                    if (newCandle.getTime() > lastTime) {
                        candles.add(newCandle);
                    } else if (newCandle.getTime() == lastTime) {
                        candles.set(candles.size() - 1, newCandle);
                    }

                    if (candles.size() > 500) {
                        candles.remove(0); // 오래된 것 제거
                    }
                }

                // 지표 계산
                if (candles.size() >= 50) {
                    List<IndicatorUtil.IndicatorPoint> rsi = IndicatorUtil.calculateRSI(candles, 14);
                    List<IndicatorUtil.DualIndicatorPoint> stoch = IndicatorUtil.calculateStochRSI(candles, 14, 3);
                    IndicatorUtil.VWBB vwbb = IndicatorUtil.calculateVWBB(candles, 20, 2);
                    IndicatorCache cache = new IndicatorCache(candles, rsi, stoch, vwbb, candles.get(candles.size() - 1).getClose());
                    IndicatorMemoryStore.put(SYMBOL + "_" + timeframe, cache);

                    log.debug("✅ [{}] 지표 갱신 완료: RSI {}, Stoch {}, VWBB.Basis {}",
                            timeframe,
                            rsi.get(rsi.size() - 1).getValue(),
                            stoch.get(stoch.size() - 1).getK(),
                            vwbb.getBasis().get(vwbb.getBasis().size() - 1).getValue());
                }

            } catch (Exception e) {
                log.error("❌ [{}] 지표 갱신 실패", timeframe, e);
            }
        }
    }

    private List<CandleDto> toCandleList(Object[][] arr) {
        List<CandleDto> result = new ArrayList<>();
        for (Object[] o : arr) {
            CandleDto c = new CandleDto();
            c.setTime(((Number) o[0]).longValue());
            c.setOpen(Double.parseDouble((String) o[1]));
            c.setHigh(Double.parseDouble((String) o[2]));
            c.setLow(Double.parseDouble((String) o[3]));
            c.setClose(Double.parseDouble((String) o[4]));
            c.setVolume(Double.parseDouble((String) o[5]));
            result.add(c);
        }
        return result;
    }
}
