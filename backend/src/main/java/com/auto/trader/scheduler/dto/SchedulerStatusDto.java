// 📁 com.auto.trader.scheduler.dto.SchedulerStatusDto.java

package com.auto.trader.scheduler.dto;

import com.auto.trader.scheduler.enums.SchedulerType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerStatusDto {
	private SchedulerType type;
	private boolean enabled;
	private boolean log;
}
