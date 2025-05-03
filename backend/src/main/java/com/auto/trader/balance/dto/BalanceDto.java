
package com.auto.trader.balance.dto;

public class BalanceDto {
    private String asset;
    private double available;
    private double locked;
    private double total;
    private double usdValue;
    private double krwValue;

    public BalanceDto(String asset, double available, double locked, double total, double usdValue, double krwValue) {
        this.asset = asset;
        this.available = available;
        this.locked = locked;
        this.total = total;
        this.usdValue = usdValue;
        this.krwValue = krwValue;
    }

    public String getAsset() {
        return asset;
    }

    public double getAvailable() {
        return available;
    }

    public double getLocked() {
        return locked;
    }

    public double getTotal() {
        return total;
    }

    public double getUsdValue() {
        return usdValue;
    }

    public double getKrwValue() {
        return krwValue;
    }
}
