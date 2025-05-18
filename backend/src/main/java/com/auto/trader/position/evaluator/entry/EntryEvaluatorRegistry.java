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
	}

	public static EntryConditionEvaluator get(IndicatorType type) {
		return evaluators.get(type);
	}
}