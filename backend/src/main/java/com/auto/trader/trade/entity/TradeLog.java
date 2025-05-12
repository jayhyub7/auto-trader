// 📁 com.auto.trader.trade.entity.TradeLog.java

package com.auto.trader.trade.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auto.trader.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trade_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String positionTitle;
  private String exchange;
  private String symbol;
  private String direction;
  private double quantity;

  private boolean success;
  private String orderId;
  private double price;

  @Column(columnDefinition = "TEXT")
  private String response;

  private String errorMessage;

  // ✅ 주문 당시 만족했던 조건들 (스냅샷으로 저장)
  @OneToMany(mappedBy = "tradeLog", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TradeCondition> conditions = new ArrayList<>();

  @Column(nullable = false)
  private boolean executed = false;

  private LocalDateTime executedAt;
}
