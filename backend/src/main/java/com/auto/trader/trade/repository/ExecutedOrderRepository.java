package com.auto.trader.trade.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.auto.trader.trade.entity.ExecutedOrder;

@Repository
public interface ExecutedOrderRepository extends JpaRepository<ExecutedOrder, Long> {
  Optional<ExecutedOrder> findByOrderId(String orderId);
}
