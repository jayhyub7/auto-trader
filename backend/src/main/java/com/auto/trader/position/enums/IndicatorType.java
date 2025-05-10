package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IndicatorType {
    RSI,
    STOCH_RSI,
    VWBB,
    STOP_HUNTING,
    EMA_CROSS,
    MACD,
    VOLUME_SPIKE,
    ATR_BREAKOUT,
    CCI,
    MA_SLOPE;

    @JsonValue
    public String toJson() {
        return name();
    }
}
