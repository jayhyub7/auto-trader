// 📁 com.auto.trader.scheduler.repository.SchedulerStatusRepository.java

package com.auto.trader.scheduler.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auto.trader.scheduler.entity.SchedulerStatus;
import com.auto.trader.scheduler.enums.SchedulerType;

public interface SchedulerStatusRepository extends JpaRepository<SchedulerStatus, SchedulerType> {
	Optional<SchedulerStatus> findByType(SchedulerType type);
}
