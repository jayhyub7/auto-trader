package com.auto.trader.position.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.auto.trader.domain.User;
import com.auto.trader.position.dto.PositionDto;
import com.auto.trader.position.dto.PositionDto.IndicatorConditionDto;
import com.auto.trader.position.entity.IndicatorCondition;
import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;
import com.auto.trader.position.enums.PositionOpenStatus;
import com.auto.trader.position.repository.PositionOpenRepository;
import com.auto.trader.position.repository.PositionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionService {

	private final PositionRepository positionRepository;
	private final PositionOpenRepository positionOpenRepository;

	public List<PositionDto> findAll() {
		return positionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
	}

	public List<PositionDto> findAllByUser(User user) {
		List<Position> positions = positionRepository.findAllByUserWithOpen(user);
		return positions.stream().map(this::toDto).collect(Collectors.toList());
	}

	@Transactional
	public List<PositionDto> saveAll(List<PositionDto> dtos, User user) {
		List<PositionDto> result = new ArrayList<>();

		for (PositionDto dto : dtos) {
			Position position = toEntity(dto, user);

			// 신규 포지션: 그냥 저장
			if (dto.getId() == null) {
				Position saved = positionRepository.save(position);
				result.add(toDto(saved));
				continue;
			}

			// 기존 포지션: 열려있는 상태인지 체크 후 저장
			Optional<PositionOpen> openOpt = positionOpenRepository.findByPosition(position);

			boolean isBlocked = openOpt
				.map(open -> open.getStatus() == PositionOpenStatus.RUNNING
						|| open.getStatus() == PositionOpenStatus.PENDING)
				.orElse(false);

			if (isBlocked) {
				log.info("사용중인 포지션 제외");
				continue; // 저장 제외
			}

			Position saved = positionRepository.save(position);
			result.add(toDto(saved));
		}

		return result;
	}

	private Position toEntity(PositionDto dto, User user) {
		Position position;

		if (dto.getId() != null) {
			// ✅ 기존 포지션 조회 및 필드 업데이트
			position = positionRepository
				.findById(dto.getId())
				.orElseThrow(() -> new RuntimeException("존재하지 않는 포지션 ID: " + dto.getId()));

			position.setTitle(dto.getTitle());
			position.setExchange(dto.getExchange());
			position.setDirection(dto.getDirection());
			position.setEnabled(dto.isEnabled());
			position.setUser(user);

			updateConditions(position, dto.getConditions());
		} else {
			// ✅ 새 포지션 생성
			position = Position
				.builder()
				.title(dto.getTitle())
				.exchange(dto.getExchange())
				.direction(dto.getDirection())
				.enabled(dto.isEnabled())
				.user(user)
				.build();

			List<IndicatorCondition> conditions = dto
				.getConditions()
				.stream()
				.map(cond -> IndicatorCondition
					.builder()
					.type(cond.getType())
					.value(cond.getValue())
					.k(cond.getK())
					.d(cond.getD())
					.operator(cond.getOperator())
					.timeframe(cond.getTimeframe())
					.conditionPhase(cond.getConditionPhase())
					.enabled(cond.isEnabled())
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
		Set<Long> newIds = newDtos
			.stream()
			.map(IndicatorConditionDto::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		// 1. 삭제: 기존에 있는데 DTO에 없는 항목
		existing.removeIf(cond -> cond.getId() != null && !newIds.contains(cond.getId()));

		// 2. 추가/갱신
		for (IndicatorConditionDto dto : newDtos) {
			if (dto.getId() != null) {
				IndicatorCondition target = existing
					.stream()
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
				target.setConditionPhase(dto.getConditionPhase());
				target.setEnabled(dto.isEnabled());
			} else {
				IndicatorCondition newCond = IndicatorCondition
					.builder()
					.type(dto.getType())
					.value(dto.getValue())
					.k(dto.getK())
					.d(dto.getD())
					.operator(dto.getOperator())
					.timeframe(dto.getTimeframe())
					.conditionPhase(dto.getConditionPhase())
					.enabled(dto.isEnabled())
					.position(position)
					.build();
				existing.add(newCond);
			}
		}
	}

	private PositionDto toDto(Position p) {
		return PositionDto
			.builder()
			.id(p.getId())
			.title(p.getTitle())
			.exchange(p.getExchange())
			.direction(p.getDirection())
			.enabled(p.isEnabled())
			.conditions(p
				.getConditions()
				.stream()
				.map(c -> PositionDto.IndicatorConditionDto
					.builder()
					.id(c.getId())
					.type(c.getType())
					.value(c.getValue())
					.k(c.getK())
					.d(c.getD())
					.operator(c.getOperator())
					.timeframe(c.getTimeframe())
					.conditionPhase(c.getConditionPhase())
					.enabled(c.getEnabled())
					.build())
				.toList())
			.open(positionOpenRepository
				.findTopByPositionOrderByCreatedAtDesc(p)
				.map(open -> PositionDto.PositionOpenDto
					.builder()
					.id(open.getId())
					.positionId(p.getId())
					.status(open.getStatus().name()) // 예: "RUNNING"
					.amountType(open.getAmountType().name())
					.amount(open.getAmount())
					.stopLoss(open.getStopLoss())
					.takeProfit(open.getTakeProfit())
					.build())
				.orElse(null))
			.build();
	}

	public void deleteById(Long id) {
		Position position = positionRepository
			.findById(id)
			.orElseThrow(() -> new RuntimeException("존재하지 않는 포지션: " + id));

		Optional<PositionOpen> openOpt = positionOpenRepository.findByPosition(position);

		boolean isBlocked = openOpt
			.map(open -> open.getStatus() == PositionOpenStatus.RUNNING
					|| open.getStatus() == PositionOpenStatus.PENDING)
			.orElse(false);

		if (isBlocked) {
			throw new IllegalStateException("실행 중이거나 시뮬레이션 중인 포지션은 삭제할 수 없습니다.");
		}

		positionRepository.delete(position);
	}

	public void deleteByIds(List<Long> ids) {
		for (Long id : ids) {
			deleteById(id); // 각 포지션에 대해 상태 검사 수행
		}
	}

}
