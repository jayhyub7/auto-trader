package com.auto.trader.position.entity;

import java.util.ArrayList;
import java.util.List;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.domain.Exchange;
import com.auto.trader.domain.User;
import com.auto.trader.position.enums.Direction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	@Enumerated(EnumType.STRING)
	private Exchange exchange;

	@Enumerated(EnumType.STRING)
	private Direction direction;

	private boolean enabled;

	@Builder.Default
	@Column(name = "is_simulating", nullable = true)
	private boolean simulating = true;

	@OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IndicatorCondition> conditions = new ArrayList<>();

	@OneToMany(mappedBy = "position", fetch = FetchType.LAZY)
	private List<PositionOpen> positionOpenList;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public boolean isSimulation() {
		return this.simulating;
	}
}
