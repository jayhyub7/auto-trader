package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorType {
    RSI, StochRSI, VWBB;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}