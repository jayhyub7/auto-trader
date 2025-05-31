package com.auto.trader.position.evaluator.entry;

import java.util.List;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;
import com.auto.trader.trade.indicator.IndicatorUtil;

public class TestEvaluator implements EntryConditionEvaluator {

	private static final int MIN_CANDLES = 50;
	private static final double MAX_WICK_RATIO = 4.0;

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction,
			SchedulerLogManager log) {

		String key = "BTCUSDT_1m";
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();

		if (candles == null || candles.size() < MIN_CANDLES) {
			log.log("⚠️ (1m) 캔들 개수 부족: {}", candles != null ? candles.size() : 0);
			return false;
		}

		// 🔍 디버깅: 최근 50봉 High/Low 확인
		log.log("📌 최근 50봉 고가/저가 목록:");
		List<CandleDto> recentCandles = candles.subList(candles.size() - MIN_CANDLES, candles.size());
		for (CandleDto c : recentCandles) {
			log
				.log("🕒 time: {}, high: {}, low: {}, open: {}, close: {}", IndicatorUtil.toKST(c.getTime()),
						c.getHigh(), c.getLow(), c.getOpen(), c.getClose());
		}

		CandleDto wickCandle = candles.get(candles.size() - 2);
		CandleDto confirmCandle = candles.get(candles.size() - 1);

		double recentHigh = recentCandles.stream().mapToDouble(CandleDto::getHigh).max().orElse(Double.NaN);
		double recentLow = recentCandles.stream().mapToDouble(CandleDto::getLow).min().orElse(Double.NaN);

		log.log("📊 (1m) 기준 고점: {}, 저점: {}", (int) recentHigh, (int) recentLow);
		log
			.log("📌 wickCandle 시점: {}, high: {}, low: {}", IndicatorUtil.toKST(wickCandle.getTime()),
					wickCandle.getHigh(), wickCandle.getLow());

		boolean stopTriggered = false;
		if (direction == Direction.SHORT && wickCandle.getHigh() >= recentHigh) {
			stopTriggered = true;
		} else if (direction == Direction.LONG && wickCandle.getLow() <= recentLow) {
			stopTriggered = true;
		}

		if (!stopTriggered) {
			log.log("❌ 고/저점 돌파 안됨 → 스탑헌팅 조건 불충족");
			return false;
		}

		double body = Math.abs(wickCandle.getClose() - wickCandle.getOpen());
		if (body < 1e-8) {
			log.log("❌ body=0 도지형 → 제외");
			return false;
		}

		double upperWick = wickCandle.getHigh() - Math.max(wickCandle.getClose(), wickCandle.getOpen());
		double lowerWick = Math.min(wickCandle.getClose(), wickCandle.getOpen()) - wickCandle.getLow();

		if (direction == Direction.SHORT && upperWick > body * MAX_WICK_RATIO) {
			log.log("❌ 윗꼬리 비율 과다 (upperWick: {}, body: {})", (int) upperWick, (int) body);
			return false;
		}
		if (direction == Direction.LONG && lowerWick > body * MAX_WICK_RATIO) {
			log.log("❌ 아랫꼬리 비율 과다 (lowerWick: {}, body: {})", (int) lowerWick, (int) body);
			return false;
		}

		boolean reverted = false;
		if (direction == Direction.SHORT) {
			if (confirmCandle.getClose() < wickCandle.getOpen()) {
				reverted = true;
			} else {
				log
					.log("❌ 되돌림 실패 (SHORT) → confirm 종가({}) >= wick 시가({})", confirmCandle.getClose(),
							wickCandle.getOpen());
			}
		} else if (direction == Direction.LONG) {
			if (confirmCandle.getClose() > wickCandle.getOpen()) {
				reverted = true;
			} else {
				log
					.log("❌ 되돌림 실패 (LONG) → confirm 종가({}) <= wick 시가({})", confirmCandle.getClose(),
							wickCandle.getOpen());
			}
		}

		if (!reverted) {
			log.log("❌ 되돌림 실패 → 진입 불가");
			return false;
		}

		log.log("✅ (1m) 스탑헌팅 조건 충족: 꼬리 + 복귀 확인");
		return true;
	}
}
