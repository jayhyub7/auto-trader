package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorType {

	// ───── 지표 유형 ─────
	RSI("RSI", ConditionType.INDICATOR), STOCH_RSI("STOCH_RSI", ConditionType.INDICATOR),
	VWBB("VWBB", ConditionType.INDICATOR),

	// ───── 매매법 유형 ─────
	STOP_HUNTING("스탑헌팅", ConditionType.STRATEGY), FIVE_CANDLE("5캔들", ConditionType.STRATEGY),
	TEST("테스트", ConditionType.STRATEGY), STOP_HUNTING_1M("스탑헌팅", ConditionType.STRATEGY);

	private final String label;
	private final ConditionType conditionType;

	IndicatorType(String label, ConditionType conditionType) {
		this.label = label;
		this.conditionType = conditionType;
	}

	public ConditionType getConditionType() {
		return conditionType;
	}

	@JsonValue
	public String toJson() {
		return label;
	}

	@JsonCreator
	public static IndicatorType from(String input) {
		for (IndicatorType type : values()) {
			if (type.name().equalsIgnoreCase(input) || type.label.equalsIgnoreCase(input)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown IndicatorType: " + input);
	}

	@Override
	public String toString() {
		return label;
	}
}
