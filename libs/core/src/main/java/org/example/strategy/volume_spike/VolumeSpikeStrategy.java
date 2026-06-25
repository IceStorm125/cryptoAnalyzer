package org.example.strategy.volume_spike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.SignalType;

@Getter
public class VolumeSpikeStrategy {

    private static final int DEFAULT_LOOKBACK_PERIOD = 48;
    private static final double DEFAULT_SPIKE_MULTIPLIER = 3.0;
    private static final int DEFAULT_SIGNAL_COOLDOWN_CANDLES = 6;

    private final int lookbackPeriod;
    private final double spikeMultiplier;
    private final int signalCooldownCandles;

    public VolumeSpikeStrategy() {
        this(DEFAULT_LOOKBACK_PERIOD, DEFAULT_SPIKE_MULTIPLIER, DEFAULT_SIGNAL_COOLDOWN_CANDLES);
    }

    public VolumeSpikeStrategy(int lookbackPeriod, double spikeMultiplier, int signalCooldownCandles) {
        validateParameters(lookbackPeriod, spikeMultiplier, signalCooldownCandles);
        this.lookbackPeriod = lookbackPeriod;
        this.spikeMultiplier = spikeMultiplier;
        this.signalCooldownCandles = signalCooldownCandles;
    }

    public List<VolumeSpikeSignal> findSignals(List<OhlcCandleDto> candles) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() <= lookbackPeriod) {
            return new ArrayList<>();
        }

        List<VolumeSpikeSignal> signals = new ArrayList<>();
        int lastSignalIndex = -1;

        for (int currentIndex = lookbackPeriod; currentIndex < candles.size(); currentIndex++) {
            OhlcCandleDto currentCandle = candles.get(currentIndex);
            double currentVolume = currentCandle.getVolumeto();
            double baselineVolume = medianVolumeTo(candles, currentIndex - lookbackPeriod, currentIndex);

            if (baselineVolume <= 0 || currentVolume <= 0) {
                continue;
            }

            boolean cooldownActive = lastSignalIndex >= 0
                    && currentIndex - lastSignalIndex <= signalCooldownCandles;
            double volumeRatio = currentVolume / baselineVolume;

            if (volumeRatio >= spikeMultiplier && !cooldownActive) {
                SignalType signalType = classifyByCandleDirection(currentCandle);
                if (signalType == null) {
                    continue;
                }

                signals.add(new VolumeSpikeSignal(
                        currentCandle.getTime(),
                        currentCandle.getClose(),
                        currentVolume,
                        baselineVolume,
                        volumeRatio,
                        signalType
                ));
                lastSignalIndex = currentIndex;
            }
        }

        return signals;
    }

    private double medianVolumeTo(List<OhlcCandleDto> candles, int fromInclusive, int toExclusive) {
        List<Double> volumes = new ArrayList<>(toExclusive - fromInclusive);
        for (int index = fromInclusive; index < toExclusive; index++) {
            double volume = candles.get(index).getVolumeto();
            if (volume > 0) {
                volumes.add(volume);
            }
        }

        if (volumes.isEmpty()) {
            return 0;
        }

        Collections.sort(volumes);
        int middleIndex = volumes.size() / 2;
        if (volumes.size() % 2 == 1) {
            return volumes.get(middleIndex);
        }

        return (volumes.get(middleIndex - 1) + volumes.get(middleIndex)) / 2.0;
    }

    private SignalType classifyByCandleDirection(OhlcCandleDto candle) {
        if (candle.getClose() > candle.getOpen()) {
            return SignalType.BUY;
        }
        if (candle.getClose() < candle.getOpen()) {
            return SignalType.SELL;
        }

        return null;
    }

    private void validateParameters(int lookbackPeriod, double spikeMultiplier, int signalCooldownCandles) {
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
