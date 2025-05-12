package com.auto.trader.trade.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.auto.trader.trade.entity.ExecutedIndicator;

@Repository
public interface ExecutedIndicatorRepository extends JpaRepository<ExecutedIndicator, Long> {
  // 예: 특정 orderId로 찾기
  List<ExecutedIndicator> findByExecutedOrderId(Long executedOrderId);
}
