package com.auto.trader.scheduler.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SchedulerType {
	ENTRY, EXIT, BALANCE, INDICATOR, FX;

	@JsonValue
	public String toJson() {
		return name(); // 대문자 그대로 반환
	}
}