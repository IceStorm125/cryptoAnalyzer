package org.example.strategy.rsi_reversal;

public record RsiOversoldStrategySettings(int rsiPeriod, double oversoldLevel, double overboughtLevel) {

    public RsiOversoldStrategySettings {
        if (rsiPeriod <= 0) {
            throw new IllegalArgumentException("RSI period must be greater than 0");
        }
        if (oversoldLevel <= 0 || overboughtLevel >= 100 || oversoldLevel >= overboughtLevel) {
            throw new IllegalArgumentException("RSI levels are invalid");
        }
    }
}
