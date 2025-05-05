package com.auto.trader.position.dto;

import java.util.List;

import com.auto.trader.domain.Exchange;
import com.auto.trader.position.enums.ConditionPhase;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.IndicatorType;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Timeframe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionDto {
    private Long id;
    private String title;
    private Exchange exchange;
    private boolean enabled;
    private List<IndicatorConditionDto> conditions;

    private Long userId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndicatorConditionDto {
        private Long id;
        private IndicatorType type;
        private Double value;
        private Double k;
        private Double d;
        private Operator operator;
        private Timeframe timeframe;
        private Direction direction;
        private ConditionPhase conditionPhase; 
    }
}

