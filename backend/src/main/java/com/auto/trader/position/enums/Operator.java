package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Operator {
    @JsonProperty("이상")
    이상,

    @JsonProperty("이하")
    이하,

    @JsonProperty("상단 돌파")
    상단_돌파,

    @JsonProperty("하단 돌파")
    하단_돌파
}