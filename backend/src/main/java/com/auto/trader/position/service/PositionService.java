package com.auto.trader.position.service;

import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository positionRepository;

    public List<PositionDto> findAll() {
        return positionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<PositionDto> saveAll(List<PositionDto> dtos) {
        List<Position> saved = positionRepository.saveAll(
            dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList())
        );

        return saved.stream()
            .map(this::toDto) // 실제 ID 포함된 DTO 변환
            .collect(Collectors.toList());
    }

    private PositionDto toDto(Position p) {
        return PositionDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .exchange(p.getExchange())
                .enabled(p.isEnabled())
                .conditions(p.getConditions().stream().map(c -> PositionDto.IndicatorConditionDto.builder()
                        .id(c.getId())
                        .type(c.getType())
                        .value(c.getValue())
                        .k(c.getK())
                        .d(c.getD())
                        .operator(c.getOperator())
                        .timeframe(c.getTimeframe())
                        .direction(c.getDirection())
                        .build()).toList())
                .build();
    }

    private Position toEntity(PositionDto dto) {
        Position position = Position.builder()
                .title(dto.getTitle())
                .exchange(dto.getExchange())
                .enabled(dto.isEnabled())
                .build();

        List<IndicatorCondition> conditions = dto.getConditions().stream().map(cond -> IndicatorCondition.builder()
                .type(cond.getType())
                .value(cond.getValue())
                .k(cond.getK())
                .d(cond.getD())
                .operator(cond.getOperator())
                .timeframe(cond.getTimeframe())
                .direction(cond.getDirection())
                .position(position)
                .build()).toList();

        position.setConditions(conditions);
        return position;
    }
}
