// ✅ 통합된 지표 처리 구성 (WebSocket + 캔들 생성 + 지표 계산)

package com.auto.trader.trade.indicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.EnableScheduling;
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

	private static final List<String> TIMEFRAMES = List.of("1m", "3m", "5m", "15m", "1h", "4h");
	private static final Map<String, Long> INTERVAL_MILLIS = Map
		.of("1m", 60_000L, "3m", 3 * 60_000L, "5m", 5 * 60_000L, "15m", 15 * 60_000L, "1h", 60 * 60_000L, "4h",
				4 * 60 * 60_000L, "1d", 24 * 60 * 60_000L);

	private final Map<String, List<CandleDto>> candleMap = new HashMap<>();
	private final Map<String, CandleDto> currentCandleMap = new HashMap<>();

	@PostConstruct
	public void init() throws InterruptedException {
		String symbol = "BTCUSDT";
		for (String tf : TIMEFRAMES) {
			List<CandleDto> candles = loadInitialCandles(symbol, tf);
			candleMap.put(tf, candles);
			Thread.sleep(200);
			currentCandleMap.put(tf, null);
			indicatorCalculator.calculateAndStore(symbol, tf, candles);
		}
	}

	private List<CandleDto> loadInitialCandles(String symbol, String timeframe) {
		try {
			long interval = INTERVAL_MILLIS.get(timeframe);
			long now = System.currentTimeMillis();
			long endTime = now - (now % interval);
			long startTime = endTime - 500 * interval;

			// ✅ 선물 기준 REST API 호출
			String url = String
				.format("https://fapi.binance.com/fapi/v1/klines?symbol=%s&interval=%s&startTime=%d&endTime=%d", symbol,
						timeframe, startTime, endTime);

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

			log
				.info("📥 [선물] 초기 캔들 불러오기 완료 [{}]: {}개 | start={}, end={}", timeframe, result.size(), startTime,
						endTime);
			return result;

		} catch (Exception e) {
			log.error("❌ [선물] 초기 캔들 로드 실패 [{}]", timeframe, e);
			return new ArrayList<>();
		}
	}

	public void handleCandle(String symbol, String timeframe, long time, double open, double high, double low,
			double close, double volume, boolean isFinal) {
		try {
			List<CandleDto> candles = candleMap.get(timeframe);
			if (candles == null) {
				candles = new ArrayList<>();
				candleMap.put(timeframe, candles);
			}

			CandleDto newCandle = CandleDto
				.builder()
				.time(time)
				.open(open)
				.high(high)
				.low(low)
				.close(close)
				.volume(volume)
				.isFinal(isFinal) // ✅ final 여부 저장
				.build();

			if (candles.isEmpty()) {
				candles.add(newCandle);
				return;
			}

			CandleDto last = candles.getLast();

			// 새로운 캔들이 생성되었고, 이전 캔들이 마감됨
			if (newCandle.getTime() > last.getTime() && last.isFinal()) {
				candles.add(newCandle);
				// 현재 캔들 진행중이면 현재캔들에 덮어쓰기
			} else if (newCandle.getTime() == last.getTime()) {
				candles.set(candles.size() - 1, newCandle);
			}

			indicatorCalculator.calculateAndStore(symbol, timeframe, candles);

		} catch (Exception e) {
			log.error("❌ handleCandle 처리 실패", e);
		}
	}

	private double roundToDecimals(double value, int decimals) {
		double scale = Math.pow(10, decimals);
		return Math.round(value * scale) / scale;
	}
}
