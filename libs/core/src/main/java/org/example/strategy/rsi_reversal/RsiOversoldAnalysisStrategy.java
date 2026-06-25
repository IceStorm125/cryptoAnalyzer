package org.example.strategy.rsi_reversal;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.StrategyResult;

public class RsiOversoldAnalysisStrategy implements AnalysisStrategy {

    private static final String STRATEGY_NAME = "RSI reversal";

    private final RsiOversoldStrategy strategy;

    public RsiOversoldAnalysisStrategy(RsiOversoldStrategySettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("RSI settings must not be null");
        }

        this.strategy = new RsiOversoldStrategy(
                settings.rsiPeriod(),
                settings.oversoldLevel(),
                settings.overboughtLevel()
        );
    }

    @Override
    public String getName() {
        return STRATEGY_NAME;
    }

    @Override
    public StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles) {
        List<String> descriptions = strategy.findSignals(candles).stream()
                .map(signal -> "RSI time=" + Instant.ofEpochSecond(signal.getCandleTime())
                        + ", type=" + signal.getSignalType()
                        + ", close=" + signal.getClosePrice()
                        + ", rsi=" + signal.getRsi())
                .collect(Collectors.toList());

        return new StrategyResult(getName(), asset, candles.size(), descriptions);
    }
}
