package com.auto.trader.position.dto;

import com.auto.trader.domain.Exchange;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.IndicatorType;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Timeframe;
import lombok.*;

import java.util.List;

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
    }
}
