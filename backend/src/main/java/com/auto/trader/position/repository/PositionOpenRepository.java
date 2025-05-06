package com.auto.trader.position.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.position.entity.Position;
import com.auto.trader.position.entity.PositionOpen;

public interface PositionOpenRepository extends JpaRepository<PositionOpen, Long> {
    Optional<PositionOpen> findByPosition(Position position);
    List<PositionOpen> findAllByPositionUserId(Long userId);
}