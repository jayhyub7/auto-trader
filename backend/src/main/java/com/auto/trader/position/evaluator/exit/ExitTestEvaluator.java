package com.auto.trader.position.evaluator.exit;

import java.util.List;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;
import com.auto.trader.trade.indicator.IndicatorMemoryStore;

public class ExitTestEvaluator implements ExitConditionEvaluator {

	private static final double PROFIT_RATIO = 0.005; // 0.5% 익절 기준

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache _unused, Direction direction, double entryPrice,
			SchedulerLogManager log) {

		String key = "BTCUSDT_1m";
		IndicatorCache cache = IndicatorMemoryStore.get(key);
		List<CandleDto> candles = cache != null ? cache.getCandles() : null;

		if (candles == null || candles.size() < 2) {
			log.log("⚠️ [ExitTestEvaluator - 1m] 캔들 부족");
			return false;
		}

		CandleDto current = candles.get(candles.size() - 1);

		double targetPrice = direction == Direction.LONG ? entryPrice + entryPrice * PROFIT_RATIO
				: entryPrice - entryPrice * PROFIT_RATIO;

		if (direction == Direction.LONG && current.getHigh() >= targetPrice) {
			log.log("✅ [ExitTestEvaluator - 1m] LONG 익절: high={} ≥ target={}", current.getHigh(), targetPrice);
			return true;
		}

		if (direction == Direction.SHORT && current.getLow() <= targetPrice) {
			log.log("✅ [ExitTestEvaluator - 1m] SHORT 익절: low={} ≤ target={}", current.getLow(), targetPrice);
			return true;
		}

		log.log("[ExitTestEvaluator - 1m] 익절 미충족 (목표가: {})", targetPrice);
		return false;
	}
}
