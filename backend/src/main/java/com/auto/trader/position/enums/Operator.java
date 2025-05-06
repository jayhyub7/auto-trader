package com.auto.trader.position.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Operator {

    이상("이상"),
    이하("이하"),
    상단_돌파("상단 돌파"),
    하단_돌파("하단 돌파");

    private final String label;

    Operator(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Operator fromLabel(String input) {
        for (Operator op : values()) {
            if (op.label.equals(input) || op.name().equalsIgnoreCase(input)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + input);
    }
}
