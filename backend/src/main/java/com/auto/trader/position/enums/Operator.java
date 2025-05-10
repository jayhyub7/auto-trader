package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Operator {

    이상,
    이하,
    상단_돌파,
    하단_돌파;

    @JsonValue
    public String toJson() {
        return name(); // 그대로: "상단_돌파", "이상" 등
    }

    @JsonCreator
    public static Operator fromJson(String value) {
        return Operator.valueOf(value);
    }
}
