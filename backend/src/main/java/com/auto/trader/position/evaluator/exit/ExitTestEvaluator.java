// ✅ ExitTestEvaluator.java (0.5% 익절 조건)
package com.auto.trader.position.evaluator.exit;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.dto.CandleDto;
import com.auto.trader.trade.indicator.IndicatorCache;

public class ExitTestEvaluator implements ExitConditionEvaluator {

	private static final double PROFIT_RATIO = 0.005; // 0.5% 익절 조건

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, Direction direction, double entryPrice,
			SchedulerLogManager log) {

		if (cache == null || cache.getCandles().size() < 2) {
			log.log("[ExitTestEvaluator] 캔들 부족");
			return false;
		}

		CandleDto entry = cache.getCandles().get(cache.getCandles().size() - 2);
		CandleDto current = cache.getCandles().get(cache.getCandles().size() - 1);

		double targetPrice = direction == Direction.LONG ? entryPrice + entryPrice * PROFIT_RATIO
				: entryPrice - entryPrice * PROFIT_RATIO;

		if (direction == Direction.LONG && current.getHigh() >= targetPrice) {
			log.log("✅ [ExitTestEvaluator] LONG 익절 기준 충족: high={} ≥ target={}", current.getHigh(), targetPrice);
			return true;
		}

		if (direction == Direction.SHORT && current.getLow() <= targetPrice) {
			log.log("✅ [ExitTestEvaluator] SHORT 익절 기준 충족: low={} ≤ target={}", current.getLow(), targetPrice);
			return true;
		}

		log.log("[ExitTestEvaluator] 익절 기준 미충족");
		return false;
	}
}