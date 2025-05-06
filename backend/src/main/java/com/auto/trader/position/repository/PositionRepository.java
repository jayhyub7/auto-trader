package com.auto.trader.position.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auto.trader.domain.User;
import com.auto.trader.position.entity.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {
	List<Position> findAllByUser(User user);
	
	@Query("""
		    SELECT p FROM Position p
		    LEFT JOIN FETCH p.positionOpenList po
		    WHERE p.user = :user
		""")
		List<Position> findAllWithOpenByUser(@Param("user") User user);
	
	@Query("""
		    SELECT DISTINCT p
		    FROM Position p
		    LEFT JOIN FETCH p.positionOpenList o
		    LEFT JOIN FETCH p.user u
		    WHERE p.enabled = true
		    AND o IS NOT NULL
		""")
		List<Position> findEnabledPositionsWithOpen();

	
}
