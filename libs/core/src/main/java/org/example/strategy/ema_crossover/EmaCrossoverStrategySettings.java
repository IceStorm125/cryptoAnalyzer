package org.example.strategy.ema_crossover;

public record EmaCrossoverStrategySettings(
        int fastPeriod,
        int slowPeriod,
        double retestTolerancePercent,
        int retestWindowCandles
) {

    public EmaCrossoverStrategySettings(int fastPeriod, int slowPeriod) {
        this(fastPeriod, slowPeriod, 0.5, Integer.MAX_VALUE);
    }

    public EmaCrossoverStrategySettings(int fastPeriod, int slowPeriod, double retestTolerancePercent) {
        this(fastPeriod, slowPeriod, retestTolerancePercent, Integer.MAX_VALUE);
    }

    public EmaCrossoverStrategySettings {
        if (fastPeriod <= 0 || slowPeriod <= 0) {
            throw new IllegalArgumentException("EMA periods must be greater than 0");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("Fast EMA period must be less than slow EMA period");
        }
        if (retestTolerancePercent < 0) {
            throw new IllegalArgumentException("EMA retest tolerance percent must not be negative");
        }
        if (retestWindowCandles <= 0) {
            throw new IllegalArgumentException("EMA retest window candles must be greater than 0");
        }
    }
}
