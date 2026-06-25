package org.example.strategy.volume_spike;

public record VolumeSpikeStrategySettings(
        int lookbackPeriod,
        double spikeMultiplier,
        int signalCooldownCandles
) {

    public VolumeSpikeStrategySettings {
        if (lookbackPeriod <= 0) {
            throw new IllegalArgumentException("Volume spike lookback period must be greater than 0");
        }
        if (spikeMultiplier <= 1.0) {
            throw new IllegalArgumentException("Volume spike multiplier must be greater than 1");
        }
        if (signalCooldownCandles < 0) {
            throw new IllegalArgumentException("Volume spike cooldown candles must not be negative");
        }
    }
}
