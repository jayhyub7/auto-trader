package com.auto.trader.position.evaluator.entry;

import java.util.List;

import org.springframework.stereotype.Component;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FiveCandleEntryEvaluator implements EntryConditionEvaluator {

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction,
			SchedulerLogManager logManager) {
		String key = "BTCUSDT_15m";

		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();
		if (candles == null || candles.size() < 10) {
			logManager.log("[오캔들] 캔들 부족");
			return false;
		}

		CandleDto current = candles.get(candles.size() - 1);
		int currentIndex = candles.size() - 1;

		// 과거부터 오캔들 구간을 탐색 (최근 순으로 뒤에서 앞으로)
		for (int i = 5; i <= currentIndex - 1; i++) {
			boolean allUp = true;
			boolean allDown = true;

			for (int j = i - 5; j < i; j++) {
				CandleDto c = candles.get(j);
				if (c.getClose() <= c.getOpen())
					allUp = false;
				if (c.getClose() >= c.getOpen())
					allDown = false;
			}

			if (!allUp && !allDown)
				continue;

			CandleDto baseCandle = candles.get(i - 5);
			double open = baseCandle.getOpen();
			double close = baseCandle.getClose();
			double top = Math.max(open, close);
			double bottom = Math.min(open, close);
			double basePrice = bottom + ((top - bottom) / 2); // 반절 기준선

			if (direction == Direction.LONG && allUp) {
				if (current.getLow() <= basePrice) {
					logManager.log("[오캔들 롱] 기준선={}, 현재 저가={}, 진입 시점 감지", basePrice, current.getLow());
					return true;
				}
			}

			if (direction == Direction.SHORT && allDown) {
				if (current.getHigh() >= basePrice) {
					logManager.log("[오캔들 숏] 기준선={}, 현재 고가={}, 진입 시점 감지", basePrice, current.getHigh());
					return true;
				}
			}
		}

		logManager.log("[오캔들] 유효한 오캔들 구간 없음 또는 기준선 미도달");
		return false;
	}
}