// ✅ 통합된 지표 처리 구성 (WebSocket + 캔들 생성 + 지표 계산)

package com.auto.trader.trade.indicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.auto.trader.trade.dto.CandleDto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class IndicatorProcessor {
	private final RestTemplate restTemplate = new RestTemplate();
	private final IndicatorCalculator indicatorCalculator;

	private static final List<String> TIMEFRAMES = List.of("1m", "3m", "5m", "15m", "1h", "4h", "1d");
	private static final Map<String, Long> INTERVAL_MILLIS = Map
		.of("1m", 60_000L, "3m", 3 * 60_000L, "5m", 5 * 60_000L, "15m", 15 * 60_000L, "1h", 60 * 60_000L, "4h",
				4 * 60 * 60_000L, "1d", 24 * 60 * 60_000L);

	private final Map<String, List<CandleDto>> candleMap = new HashMap<>();
	private final Map<String, CandleDto> currentCandleMap = new HashMap<>();

	@PostConstruct
	public void init() {
		String symbol = "BTCUSDT";
		for (String tf : TIMEFRAMES) {
			List<CandleDto> candles = loadInitialCandles(symbol, tf);
			candleMap.put(tf, candles);
			currentCandleMap.put(tf, null); // 틱 수신 시 생성됨
		}
	}

	public synchronized void handleTick(String symbol, double price, long timestamp) {
		for (String tf : TIMEFRAMES) {
			long interval = INTERVAL_MILLIS.get(tf);
			long bucketTime = timestamp - (timestamp % interval);

			List<CandleDto> candles = candleMap.computeIfAbsent(tf, k -> new ArrayList<>());
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
			} else {
				current.setClose(roundedPrice);
				current.setHigh(Math.max(current.getHigh(), roundedPrice));
				current.setLow(Math.min(current.getLow(), roundedPrice));
				current.setVolume(current.getVolume() + 1);
			}
		}
	}

	@Scheduled(fixedDelay = 1000)
	public void updateIndicators() {

		String symbol = "BTCUSDT";
		for (String tf : TIMEFRAMES) {
			List<CandleDto> candles = candleMap.getOrDefault(tf, List.of());

			if (candles.size() < 50)
				continue;

			List<CandleDto> full = new ArrayList<>(candles);
			CandleDto current = currentCandleMap.get(tf);
			if (current != null)
				full.add(current);

			indicatorCalculator.calculateAndStore(symbol, tf, full);
		}
	}

	private double roundToDecimals(double value, int decimals) {
		double scale = Math.pow(10, decimals);
		return Math.round(value * scale) / scale;
	}

	private List<CandleDto> loadInitialCandles(String symbol, String timeframe) {
		try {
			long now = System.currentTimeMillis();
			long endTime = now - (now % 60_000); // 1분봉 정각 기준
			long startTime = endTime - 500 * 60_000; // 500분 전

			String url = String
				.format("https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&startTime=%d&endTime=%d", symbol,
						timeframe, startTime, endTime);

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
					c.setVolume(roundToDecimals(Double.parseDouble((String) o[5]), 2));
					result.add(c);
				}
			}

			log.info("📥 초기 캔들 불러오기 완료 [{}]: {}개 | start={}, end={}", timeframe, result.size(), startTime, endTime);

			return result;

		} catch (Exception e) {
			log.error("❌ 초기 캔들 로드 실패 [{}]", timeframe, e);
			return new ArrayList<>();
		}
	}
}
