package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {
    LONG, SHORT;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}