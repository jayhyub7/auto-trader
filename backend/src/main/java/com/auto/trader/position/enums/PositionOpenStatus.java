package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PositionOpenStatus {
    IDLE, RUNNING, SIMULATING, CANCELLED;

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static PositionOpenStatus fromJson(String value) {
        return PositionOpenStatus.valueOf(value.toUpperCase());
    }
}
