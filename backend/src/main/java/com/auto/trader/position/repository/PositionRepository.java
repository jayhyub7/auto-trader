package com.auto.trader.position.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.domain.User;
import com.auto.trader.position.entity.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {
	List<Position> findAllByUser(User user);
}
