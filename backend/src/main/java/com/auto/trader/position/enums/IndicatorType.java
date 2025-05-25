package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorType {

	// ───── 지표 유형 ─────
	RSI("RSI"), STOCH_RSI("STOCH_RSI"), VWBB("VWBB"),

	// ───── 매매법 유형 ─────
	STOP_HUNTING("스탑헌팅"), FIVE_CANDLE("5캔들"), TEST("테스트");

	private final String label;

	IndicatorType(String label) {
		this.label = label;
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
