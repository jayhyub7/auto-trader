// 📁 com.auto.trader.scheduler.entity.SchedulerStatus.java

package com.auto.trader.scheduler.entity;

import com.auto.trader.scheduler.enums.SchedulerType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scheduler_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerStatus {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, unique = true)
	private SchedulerType type;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false)
	private boolean logEnabled;
}
