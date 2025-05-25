package com.auto.trader.position.evaluator.exit;

import java.util.EnumMap;
import java.util.Map;

import com.auto.trader.position.enums.IndicatorType;

public class ExitEvaluatorRegistry {
	private static final Map<IndicatorType, ExitConditionEvaluator> evaluators = new EnumMap<>(IndicatorType.class);
	static {
		evaluators.put(IndicatorType.RSI, new ExitRsiEvaluator());
		evaluators.put(IndicatorType.STOCH_RSI, new ExitStochRsiEvaluator());
		evaluators.put(IndicatorType.VWBB, new ExitVwbbEvaluator());
		evaluators.put(IndicatorType.FIVE_CANDLE, new ExitFiveCandleEvaluator());
		evaluators.put(IndicatorType.STOP_HUNTING, new ExitStopHuntingEvaluator());
	}

	public static ExitConditionEvaluator get(IndicatorType type) {
		return evaluators.get(type);
	}
}