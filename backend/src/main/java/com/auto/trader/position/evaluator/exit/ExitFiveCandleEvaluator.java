package com.auto.trader.position.evaluator.exit;

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
public class ExitFiveCandleEvaluator implements ExitConditionEvaluator {

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _cache, Direction direction,
			SchedulerLogManager logManager) {
		String key = "BTCUSDT_15m";

		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache.getCandles();
		if (candles == null || candles.size() < 10) {
			logManager.log("[오캔들 익절] 캔들 부족");
			return false;
		}

		CandleDto current = candles.get(candles.size() - 1);

		for (int i = candles.size() - 1; i >= 5; i--) {
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
			double basePrice = bottom + ((top - bottom) / 2);
			double delta = basePrice * 0.003; // ±0.3% 유연 범위

			if (direction == Direction.LONG && current.getHigh() >= basePrice - delta) {
				logManager.log("[오캔들 익절 - 롱] 기준선={}, 고가={}, 범위={} → 익절 시점 감지", basePrice, current.getHigh(), delta);
				return true;
			}

			if (direction == Direction.SHORT && current.getLow() <= basePrice + delta) {
				logManager.log("[오캔들 익절 - 숏] 기준선={}, 저가={}, 범위={} → 익절 시점 감지", basePrice, current.getLow(), delta);
				return true;
			}
		}

		logManager.log("[오캔들 익절] 유효한 기준선 없음 또는 익절 조건 미충족");
		return false;
	}
}
