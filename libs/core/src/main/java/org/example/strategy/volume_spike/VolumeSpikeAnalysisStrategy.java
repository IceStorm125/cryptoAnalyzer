package org.example.strategy.volume_spike;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.StrategyResult;

public class VolumeSpikeAnalysisStrategy implements AnalysisStrategy {

    private static final String STRATEGY_NAME = "Volume spike";

    private final VolumeSpikeStrategySettings settings;
    private final VolumeSpikeStrategy strategy;

    public VolumeSpikeAnalysisStrategy(VolumeSpikeStrategySettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Volume spike settings must not be null");
        }

        this.settings = settings;
        this.strategy = new VolumeSpikeStrategy(
                settings.lookbackPeriod(),
                settings.spikeMultiplier(),
                settings.signalCooldownCandles()
        );
    }

    @Override
    public String getName() {
        return STRATEGY_NAME;
    }

    @Override
    public StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles) {
        List<String> descriptions = strategy.findSignals(candles).stream()
                .map(signal -> "Volume spike time=" + Instant.ofEpochSecond(signal.getCandleTime())
                        + ", type=" + signal.getSignalType()
                        + ", close=" + signal.getClosePrice()
                        + ", volumeTo=" + format(signal.getVolumeTo())
                        + ", baselineVolumeTo=" + format(signal.getBaselineVolumeTo())
                        + ", ratio=" + format(signal.getVolumeRatio())
                        + ", lookback=" + settings.lookbackPeriod()
                        + ", multiplier=" + settings.spikeMultiplier()
                        + ", cooldown=" + settings.signalCooldownCandles())
                .collect(Collectors.toList());

        return new StrategyResult(getName(), asset, candles.size(), descriptions);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.4f", value);
    }
}
