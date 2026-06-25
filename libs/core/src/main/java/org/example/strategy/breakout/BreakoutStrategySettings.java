package org.example.strategy.breakout;

public record BreakoutStrategySettings(int lookbackPeriod, int breakoutCooldownCandles) {

    public BreakoutStrategySettings {
        if (lookbackPeriod <= 0) {
            throw new IllegalArgumentException("Lookback period must be greater than 0");
        }
        if (breakoutCooldownCandles < 0) {
            throw new IllegalArgumentException("Breakout cooldown candles must not be negative");
        }
    }
}
