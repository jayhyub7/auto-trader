package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AmountType {
    FIXED, PERCENT;

    @JsonValue
    public String toJson() {
        return name();
    }
}
