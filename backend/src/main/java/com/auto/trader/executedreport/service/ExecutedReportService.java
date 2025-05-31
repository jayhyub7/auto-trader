package com.auto.trader.executedreport.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.auto.trader.domain.User;
import com.auto.trader.executedreport.dto.ExecutedReportResponseDto;
import com.auto.trader.executedreport.dto.ExecutedReportResponseDto.ConditionDto;
import com.auto.trader.trade.entity.ExecutedOrder;
import com.auto.trader.trade.entity.TradeCondition;
import com.auto.trader.trade.entity.TradeLog;
import com.auto.trader.trade.repository.ExecutedOrderRepository;
import com.auto.trader.trade.repository.TradeConditionRepository;
import com.auto.trader.trade.repository.TradeLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecutedReportService {

	private final ExecutedOrderRepository executedOrderRepository;
	private final TradeLogRepository tradeLogRepository;
	private final TradeConditionRepository tradeConditionRepository;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public List<ExecutedReportResponseDto> getUserExecutedReports(User user) {
		List<ExecutedOrder> entries = executedOrderRepository
			.findAllByUserAndSideOrderByExecutedAtDesc(user, com.auto.trader.position.enums.Side.ENTRY);
		List<ExecutedReportResponseDto> results = new ArrayList<>();
		System.out.println("entries.size : " + entries.size());
		for (ExecutedOrder entry : entries) {
			ExecutedOrder exit = executedOrderRepository
				.findByOrderIdAndSide(entry.getOrderId(), com.auto.trader.position.enums.Side.EXIT);

			// 수익률: EXIT 체결이 있을 경우에만
			Double profitRate = exit != null ? exit.getProfitPercent() : null;

			TradeLog tradeLog = tradeLogRepository.findByOrderId(entry.getOrderId());
			List<TradeCondition> conditions = tradeConditionRepository.findByTradeLogId(tradeLog.getId());

			List<ConditionDto> conditionDtos = new ArrayList<>();
			for (TradeCondition cond : conditions) {
				if (cond == null)
					continue;

				ConditionDto dto = ConditionDto
					.builder()
					.indicator(cond.getType() != null ? cond.getType().name() : "UNKNOWN")
					.operator(cond.getOperator() != null ? cond.getOperator().name() : "UNKNOWN")
					.value(cond.getValue() != null ? cond.getValue() : 0.0)
					.timeframe(cond.getTimeframe() != null ? cond.getTimeframe().name() : "UNKNOWN")
					.phase(cond.getConditionPhase() != null ? cond.getConditionPhase().name() : "UNKNOWN")
					.build();

				conditionDtos.add(dto);
			}

			ExecutedReportResponseDto report = ExecutedReportResponseDto
				.builder()
				.executedAt(entry.getExecutedAt().format(FORMATTER))
				.positionName(tradeLog.getPositionTitle())
				.direction(entry.getDirection().name())
				.executedPrice(entry.getExecutedPrice())
				.profitRate(profitRate)
				.observedPrice(entry.getObservedPrice())
				.slippage(entry.getSlippage())
				.tpSlRegistered(entry.getTpSlRegistered())
				.executionLog(entry.getExecutionLog())
				.conditions(conditionDtos)
				.build();

			results.add(report);
		}

		return results;
	}

}