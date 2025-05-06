package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Timeframe {

    ONE_MINUTE("1m"),
    THREE_MINUTE("3m"),
    FIVE_MINUTE("5m"),
    FIFTEEN_MINUTE("15m"),
    ONE_HOUR("1h"),
    FOUR_HOURS("4h");

    private final String label;

    Timeframe(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
