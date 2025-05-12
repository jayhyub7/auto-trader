package com.auto.trader.trade.repository;

import com.auto.trader.trade.entity.TradeCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeConditionRepository extends JpaRepository<TradeCondition, Long> {
}