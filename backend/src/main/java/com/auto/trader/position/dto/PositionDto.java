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
import lombok.Data;
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
    private Direction direction;
    private boolean enabled;
    private List<IndicatorConditionDto> conditions;
    private Long userId;
    private PositionOpenDto open;

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
        private ConditionPhase conditionPhase; 
        private boolean enabled; 
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionOpenDto {
        private Long id;
        private Long positionId;          // ✅ positionId 추가
        private String status;            // e.g. "IDLE"
        private String amountType;        // e.g. "fixed", "percent"
        private double amount;
        private double stopLoss;
        private Double takeProfit;
    }
}
