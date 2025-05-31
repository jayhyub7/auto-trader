package com.auto.trader.trade.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.trade.entity.TradeCondition;

public interface TradeConditionRepository extends JpaRepository<TradeCondition, Long> {
	List<TradeCondition> findByTradeLogId(Long tradeLogId);
}