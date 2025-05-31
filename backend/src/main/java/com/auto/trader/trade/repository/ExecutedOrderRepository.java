package com.auto.trader.trade.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.auto.trader.domain.User;
import com.auto.trader.position.enums.Side;
import com.auto.trader.trade.entity.ExecutedOrder;

@Repository
public interface ExecutedOrderRepository extends JpaRepository<ExecutedOrder, Long> {
	Optional<ExecutedOrder> findByOrderId(String orderId);

	List<ExecutedOrder> findAllByUserOrderByExecutedAtDesc(User user);

	List<ExecutedOrder> findAllByUserAndSideOrderByExecutedAtDesc(User user, Side side);

	ExecutedOrder findByOrderIdAndSide(String orderId, Side side);
}
