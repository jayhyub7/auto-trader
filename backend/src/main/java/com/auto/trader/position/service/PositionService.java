package com.auto.trader.position.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.dto.PositionDto.IndicatorConditionDto;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    public List<PositionDto> findAll() {
        return positionRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<PositionDto> findAllByUser(User user) {
        List<Position> positions = positionRepository.findAllByUser(user);
        return positions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<PositionDto> saveAll(List<PositionDto> dtos, User user) {
        List<Position> saved = dtos.stream()
            .map(dto -> toEntity(dto, user))
            .map(positionRepository::save)
            .collect(Collectors.toList());

        return saved.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private Position toEntity(PositionDto dto, User user) {
        Position position;

        if (dto.getId() != null) {
            // ✅ 기존 포지션 조회 및 필드 업데이트
            position = positionRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 포지션 ID: " + dto.getId()));

            position.setTitle(dto.getTitle());
            position.setExchange(dto.getExchange());
            position.setEnabled(dto.isEnabled());
            position.setUser(user);

            updateConditions(position, dto.getConditions());
        } else {
            // ✅ 새 포지션 생성
            position = Position.builder()
                .title(dto.getTitle())
                .exchange(dto.getExchange())
                .enabled(dto.isEnabled())
                .user(user)
                .build();

            List<IndicatorCondition> conditions = dto.getConditions().stream()
                .map(cond -> IndicatorCondition.builder()
                    .type(cond.getType())
                    .value(cond.getValue())
                    .k(cond.getK())
                    .d(cond.getD())
                    .operator(cond.getOperator())
                    .timeframe(cond.getTimeframe())
                    .direction(cond.getDirection())
                    .conditionPhase(cond.getConditionPhase())
                    .position(position)
                    .build())
                .toList();

            position.setConditions(conditions);
        }

        return position;
    }

    private void updateConditions(Position position, List<IndicatorConditionDto> newDtos) {
        List<IndicatorCondition> existing = position.getConditions();

        // 기존 ID 목록
        Set<Long> newIds = newDtos.stream()
            .map(IndicatorConditionDto::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 1. 삭제: 기존에 있는데 DTO에 없는 항목
        existing.removeIf(cond -> cond.getId() != null && !newIds.contains(cond.getId()));

        // 2. 추가/갱신
        for (IndicatorConditionDto dto : newDtos) {
            if (dto.getId() != null) {
                IndicatorCondition target = existing.stream()
                    .filter(c -> c.getId().equals(dto.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("조건 ID 불일치: " + dto.getId()));

                // 필드 업데이트
                target.setType(dto.getType());
                target.setValue(dto.getValue());
                target.setK(dto.getK());
                target.setD(dto.getD());
                target.setOperator(dto.getOperator());
                target.setTimeframe(dto.getTimeframe());
                target.setDirection(dto.getDirection());
                target.setConditionPhase(dto.getConditionPhase());
            } else {
                IndicatorCondition newCond = IndicatorCondition.builder()
                    .type(dto.getType())
                    .value(dto.getValue())
                    .k(dto.getK())
                    .d(dto.getD())
                    .operator(dto.getOperator())
                    .timeframe(dto.getTimeframe())
                    .direction(dto.getDirection())
                    .conditionPhase(dto.getConditionPhase())
                    .position(position)
                    .build();
                existing.add(newCond);
            }
        }
    }

    private PositionDto toDto(Position p) {
        return PositionDto.builder()
            .id(p.getId())
            .title(p.getTitle())
            .exchange(p.getExchange())
            .enabled(p.isEnabled())
            .conditions(
                p.getConditions().stream()
                    .map(c -> PositionDto.IndicatorConditionDto.builder()
                        .id(c.getId())
                        .type(c.getType())
                        .value(c.getValue())
                        .k(c.getK())
                        .d(c.getD())
                        .operator(c.getOperator())
                        .timeframe(c.getTimeframe())
                        .direction(c.getDirection())
                        .conditionPhase(c.getConditionPhase())
                        .build())
                    .toList()
            )
            .build();
    }
    
    public void deleteByIds(List<Long> ids) {
        positionRepository.deleteAllById(ids);
    }
    
    public void deleteById(Long id) {
        positionRepository.deleteById(id);
    }
}
