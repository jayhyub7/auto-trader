package com.auto.trader.position.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.dto.PositionOpenRequestDto;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.AmountType;
import com.auto.trader.position.enums.ConditionPhase;
import com.auto.trader.position.enums.PositionOpenStatus;
import com.auto.trader.position.repository.PositionOpenRepository;
import com.auto.trader.position.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionOpenService {

	private final PositionRepository positionRepository;
	private final PositionOpenRepository positionOpenRepository;

	public List<PositionDto> getOpenPositionsForUser(User user) {
		List<Position> positions = positionRepository.findAllWithOpenByUser(user);

		return positions.stream().map(position -> {
			// PositionOpen이 존재할 경우 첫 번째만 가져옴
			PositionDto.PositionOpenDto openDto = null;
			if (!position.getPositionOpenList().isEmpty()) {
				PositionOpen open = position.getPositionOpenList().get(0);
				openDto = PositionDto.PositionOpenDto
					.builder()
					.id(open.getId())
					.status(open.getStatus().name())
					.amountType(open.getAmountType().name())
					.amount(open.getAmount())
					.stopLoss(open.getStopLoss())
					.takeProfit(open.getTakeProfit())
					.leverage(open.getLeverage())
					.build();
			}

			return PositionDto
				.builder()
				.id(position.getId())
				.title(position.getTitle())
				.exchange(position.getExchange())
				.enabled(position.isEnabled())
				.direction(position.getDirection())
				.userId(position.getUser().getId())
				.open(openDto)
				.conditions(position.getConditions().stream().map(this::toConditionDto).collect(Collectors.toList()))
				.build();
		}).collect(Collectors.toList());
	}

	public PositionOpenRequestDto save(PositionOpenRequestDto dto, User user) {
		Position position = positionRepository
			.findById(dto.getPositionId())
			.orElseThrow(() -> new RuntimeException("Position 없음"));

		if (!position.getUser().getId().equals(user.getId())) {
			throw new RuntimeException("권한 없음");
		}

		PositionOpen open = PositionOpen
			.builder()
			.position(position)
			.amount(dto.getAmount())
			.amountType(AmountType.valueOf(dto.getAmountType().toUpperCase()))
			.stopLoss(dto.getStopLoss())
			.takeProfit(dto.getTakeProfit())
			.status(PositionOpenStatus.valueOf(dto.getStatus().toUpperCase()))
			.leverage(dto.getLeverage())
			.build();

		PositionOpen saved = positionOpenRepository.save(open);

		dto.setId(saved.getId());
		return dto;
	}

	public void update(PositionOpenRequestDto dto, User user) {
		PositionOpen open = positionOpenRepository
			.findById(dto.getId())
			.orElseThrow(() -> new RuntimeException("PositionOpen 없음"));
		if (!open.getPosition().getUser().getId().equals(user.getId())) {
			throw new RuntimeException("권한 없음");
		}

		open.setAmount(dto.getAmount());
		open.setAmountType(AmountType.valueOf(dto.getAmountType().toUpperCase()));
		open.setStopLoss(dto.getStopLoss());
		open.setTakeProfit(dto.getTakeProfit());
		open.setStatus(PositionOpenStatus.valueOf(dto.getStatus().toUpperCase()));
		open.setLeverage(dto.getLeverage());

		positionOpenRepository.save(open);
	}

	private PositionDto.IndicatorConditionDto toConditionDto(IndicatorCondition cond) {
		return PositionDto.IndicatorConditionDto
			.builder()
			.id(cond.getId())
			.type(cond.getType())
			.value(cond.getValue())
			.k(cond.getK())
			.d(cond.getD())
			.operator(cond.getOperator())
			.timeframe(cond.getTimeframe())
			.conditionPhase(cond.getConditionPhase())
			.enabled(cond.getEnabled())
			.build();
	}

	@Transactional(readOnly = true)
	public List<Position> findEntryPosition() {
		List<Position> positions = positionRepository.findEnabledPositionsWithOpen();

		return positions.stream().map(p -> {
			List<PositionOpen> filteredOpens = p
				.getPositionOpenList()
				.stream()
				.filter(o -> o.getStatus() == PositionOpenStatus.RUNNING
						|| o.getStatus() == PositionOpenStatus.SIMULATING)
				.toList();

			List<IndicatorCondition> filteredConditions = p
				.getConditions()
				.stream()
				.filter(c -> Boolean.TRUE.equals(c.getEnabled()))
				.toList();

			return Position
				.builder()
				.id(p.getId())
				.title(p.getTitle())
				.exchange(p.getExchange())
				.direction(p.getDirection())
				.enabled(p.isEnabled())
				.user(p.getUser())
				.positionOpenList(filteredOpens)
				.conditions(filteredConditions)
				.build();
		})
			.filter(p -> !p.getPositionOpenList().isEmpty())
			.filter(p -> p.getConditions().stream().anyMatch(c -> c.getConditionPhase() == ConditionPhase.ENTRY))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<Position> findRunningPositions() {
		List<Position> positions = positionRepository.findEnabledPositionsWithOpen();

		return positions.stream().map(p -> {
			List<PositionOpen> filteredOpens = p
				.getPositionOpenList()
				.stream()
				.filter(o -> !o.isExecuted() && (o.getStatus() == PositionOpenStatus.RUNNING
						|| o.getStatus() == PositionOpenStatus.SIMULATING))
				.toList();

			List<IndicatorCondition> filteredConditions = p
				.getConditions()
				.stream()
				.filter(c -> Boolean.TRUE.equals(c.getEnabled()))
				.filter(c -> c.getConditionPhase() == ConditionPhase.EXIT)
				.toList();

			return Position
				.builder()
				.id(p.getId())
				.title(p.getTitle())
				.exchange(p.getExchange())
				.direction(p.getDirection())
				.enabled(p.isEnabled())
				.user(p.getUser())
				.positionOpenList(filteredOpens)
				.conditions(filteredConditions)
				.build();
		}).filter(p -> !p.getPositionOpenList().isEmpty()).toList();
	}

	@Transactional
	public void deleteById(Long id, User user) {
		PositionOpen open = positionOpenRepository.findById(id).orElseThrow(() -> new RuntimeException("포지션 오픈 정보 없음"));

		if (!open.getPosition().getUser().getId().equals(user.getId())) {
			throw new SecurityException("삭제 권한 없음");
		}

		positionOpenRepository.delete(open);
	}

	public Optional<PositionOpen> findById(Long id) {
		return positionOpenRepository.findById(id);
	}
}
