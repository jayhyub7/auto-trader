package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Side {
	ENTRY, EXIT;

	@JsonValue
	public String toJson() {
		return name();
	}
}
