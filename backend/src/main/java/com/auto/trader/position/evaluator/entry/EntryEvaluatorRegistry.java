package com.auto.trader.position.evaluator.entry;

import java.util.EnumMap;
import java.util.Map;

import com.auto.trader.position.enums.IndicatorType;

public class EntryEvaluatorRegistry {
	private static final Map<IndicatorType, EntryConditionEvaluator> evaluators = new EnumMap<>(IndicatorType.class);

	static {
		evaluators.put(IndicatorType.RSI, new RsiEvaluator());
		evaluators.put(IndicatorType.STOCH_RSI, new StochRsiEvaluator());
		evaluators.put(IndicatorType.VWBB, new VwbbEvaluator());
		evaluators.put(IndicatorType.FIVE_CANDLE, new StopHuntingEvaluator());
		evaluators.put(IndicatorType.STOP_HUNTING, new StopHuntingEvaluator());
		evaluators.put(IndicatorType.STOP_HUNTING_1M, new StopHuntingEvaluator_1m());
		evaluators.put(IndicatorType.TEST, new TestEvaluator());

	}

	public static EntryConditionEvaluator get(IndicatorType type) {
		return evaluators.get(type);
	}
}