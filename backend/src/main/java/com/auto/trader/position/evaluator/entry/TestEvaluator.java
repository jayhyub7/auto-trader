package com.auto.trader.position.evaluator.entry;

import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.scheduler.SchedulerLogManager;
import com.auto.trader.trade.indicator.IndicatorCache;

public class TestEvaluator implements EntryConditionEvaluator {

	@Override
	public boolean evaluate(IndicatorCondition cond, IndicatorCache cache, Direction direction,
			SchedulerLogManager log) {
		log.log("✅ [TestEvaluator] 무조건 진입 조건 통과");
		return true;
	}
}