package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConditionPhase {
    ENTRY, EXIT;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}