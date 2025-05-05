package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Timeframe {

    @JsonProperty("1m")
    ONE_MINUTE,

    @JsonProperty("3m")
    THREE_MINUTE,

    @JsonProperty("5m")
    FIVE_MINUTE,

    @JsonProperty("15m")
    FIFTEEN_MINUTE,

    @JsonProperty("1h") 
    ONE_HOUR,

    @JsonProperty("4h")
    FOUR_HOURS
}
