package com.auto.trader.trade.entity;

import com.auto.trader.domain.BaseEntity;
import com.auto.trader.position.enums.Timeframe;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "executed_indicator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutedIndicator extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Timeframe timeframe; // 예: ONE_MINUTE, THREE_MINUTE 등

  private Double rsi;

  private Double stochK;
  private Double stochD;

  private Double vwbbBasis;
  private Double vwbbUpper;
  private Double vwbbLower;

  private double currentPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "executed_order_id", nullable = false)
  private ExecutedOrder executedOrder;
}
