package com.auto.trader.config.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.auto.trader.config.entity.ExceptionLog;

@Repository
public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {
}
