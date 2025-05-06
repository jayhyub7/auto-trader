package com.auto.trader.position.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.auto.trader.position.dto.PositionDto.IndicatorConditionDto;

@Data
@Builder
public class PositionOpenDetailDto {
    private Long positionId;
    private String title;
    private String exchange;
    private boolean enabled;
    private PositionOpenDto open;
    private List<IndicatorConditionDto> conditions;
}