package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PositionOpenStatus {
    IDLE, RUNNING, SIMULATING, CANCELLED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}