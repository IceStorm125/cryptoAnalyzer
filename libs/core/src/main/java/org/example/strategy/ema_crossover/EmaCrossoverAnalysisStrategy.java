package org.example.strategy.ema_crossover;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.StrategyResult;

public class EmaCrossoverAnalysisStrategy implements AnalysisStrategy {

    private static final String STRATEGY_NAME = "EMA crossover";

    private final EmaCrossoverStrategySettings settings;
    private final EmaCrossoverStrategy strategy;

    public EmaCrossoverAnalysisStrategy(EmaCrossoverStrategySettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("EMA settings must not be null");
        }

        this.settings = settings;
        this.strategy = new EmaCrossoverStrategy(
                settings.fastPeriod(),
                settings.slowPeriod(),
                settings.retestTolerancePercent(),
                settings.retestWindowCandles()
        );
    }

    @Override
    public String getName() {
        return STRATEGY_NAME;
    }

    @Override
    public StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles) {
        List<String> descriptions = strategy.findSignals(candles).stream()
                .map(signal -> "EMA time=" + Instant.ofEpochSecond(signal.getCandleTime())
                        + ", type=" + signal.getSignalType()
                        + ", close=" + signal.getClosePrice()
                        + ", emaFast=" + signal.getFastEma()
                        + ", emaSlow=" + signal.getSlowEma()
                        + ", retestDistancePercent=" + signal.getRetestDistancePercent()
                        + ", candlesAfterCrossover=" + signal.getCandlesAfterCrossover()
                        + ", retestTolerancePercent=" + settings.retestTolerancePercent()
                        + ", retestWindowCandles=" + formatRetestWindow())
                .collect(Collectors.toList());

        return new StrategyResult(getName(), asset, candles.size(), descriptions);
    }

    private String formatRetestWindow() {
        if (settings.retestWindowCandles() == Integer.MAX_VALUE) {
            return "unlimited";
        }

        return Integer.toString(settings.retestWindowCandles());
    }
}
