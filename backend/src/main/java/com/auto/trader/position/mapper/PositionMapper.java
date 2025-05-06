package com.auto.trader.position.mapper;

import com.auto.trader.position.dto.*;
import com.auto.trader.position.dto.PositionDto.IndicatorConditionDto;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public PositionOpenDetailDto toOpenDetailDto(Position position, PositionOpen open) {
        return PositionOpenDetailDto.builder()
                .positionId(position.getId())
                .title(position.getTitle())
                .exchange(position.getExchange().name())
                .enabled(position.isEnabled())
                .conditions(position.getConditions().stream().map(this::toConditionDto).toList())
                .open(open == null ? null : toOpenDto(open))
                .build();
    }

    private IndicatorConditionDto toConditionDto(IndicatorCondition cond) {
        return IndicatorConditionDto.builder()
                .id(cond.getId())
                .type(cond.getType()) // 여기가 핵심
                .operator(cond.getOperator())
                .value(cond.getValue())
                .k(cond.getK())
                .d(cond.getD())
                .timeframe(cond.getTimeframe())
                .direction(cond.getDirection())
                .conditionPhase(cond.getConditionPhase())
                .build();
    }


    private PositionOpenDto toOpenDto(PositionOpen open) {
        return PositionOpenDto.builder()
                .id(open.getId())
                .status(open.getStatus().name())
                .amountType(open.getAmountType().name())
                .amount(open.getAmount())
                .stopLoss(open.getStopLoss())
                .takeProfit(open.getTakeProfit())
                .build();
    }
}