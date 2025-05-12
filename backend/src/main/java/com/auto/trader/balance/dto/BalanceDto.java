package com.auto.trader.balance.dto;

import com.auto.trader.domain.Exchange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDto {
  private String asset;
  private double available;
  private double locked;
  private double total;
  private double usdValue;
  private Exchange exchange;
}
