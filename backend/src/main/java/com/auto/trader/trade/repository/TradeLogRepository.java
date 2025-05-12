// 📁 com.auto.trader.trade.repository.TradeConditionRepository.java

package com.auto.trader.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.auto.trader.trade.entity.TradeLog;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
}
