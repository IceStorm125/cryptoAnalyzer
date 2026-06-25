package org.example.strategy.breakout;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.StrategyResult;

public class BreakoutAnalysisStrategy implements AnalysisStrategy {

    private static final String STRATEGY_NAME = "Breakout";

    private final BreakoutStrategySettings settings;
    private final BreakoutStrategy strategy;

    public BreakoutAnalysisStrategy(BreakoutStrategySettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Breakout settings must not be null");
        }

        this.settings = settings;
        this.strategy = new BreakoutStrategy(settings.lookbackPeriod(), settings.breakoutCooldownCandles());
    }

    @Override
    public String getName() {
        return STRATEGY_NAME;
    }

    @Override
    public StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles) {
        List<String> descriptions = strategy.findSignals(candles).stream()
                .map(signal -> "Breakout time=" + Instant.ofEpochSecond(signal.getCandleTime())
                        + ", type=" + signal.getSignalType()
                        + ", close=" + signal.getClosePrice()
                        + ", level=" + signal.getBreakoutLevel()
                        + ", lookback=" + signal.getLookbackPeriod()
                        + ", cooldown=" + settings.breakoutCooldownCandles())
                .collect(Collectors.toList());

        return new StrategyResult(getName(), asset, candles.size(), descriptions);
    }
}
