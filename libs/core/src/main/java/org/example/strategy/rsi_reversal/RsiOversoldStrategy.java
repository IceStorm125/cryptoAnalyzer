package org.example.strategy.rsi_reversal;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.SignalType;

@Getter
public class RsiOversoldStrategy {

    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final double DEFAULT_OVERSOLD_LEVEL = 30.0;
    private static final double DEFAULT_OVERBOUGHT_LEVEL = 70.0;

    private final int rsiPeriod;
    private final double oversoldLevel;
    private final double overboughtLevel;

    public RsiOversoldStrategy() {
        this(DEFAULT_RSI_PERIOD, DEFAULT_OVERSOLD_LEVEL, DEFAULT_OVERBOUGHT_LEVEL);
    }

    public RsiOversoldStrategy(int rsiPeriod, double oversoldLevel, double overboughtLevel) {
        validateParameters(rsiPeriod, oversoldLevel, overboughtLevel);
        this.rsiPeriod = rsiPeriod;
        this.oversoldLevel = oversoldLevel;
        this.overboughtLevel = overboughtLevel;
    }

    public List<RsiSignal> findSignals(List<OhlcCandleDto> candles) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() <= rsiPeriod) {
            return new ArrayList<>();
        }

        List<Double> rsiValues = RsiCalculator.calculateFromCandles(candles, rsiPeriod);
        List<RsiSignal> signals = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            Double previousRsi = rsiValues.get(i - 1);
            Double currentRsi = rsiValues.get(i);

            if (previousRsi == null || currentRsi == null) {
                continue;
            }

            if (previousRsi < oversoldLevel && currentRsi > oversoldLevel) {
                signals.add(buildSignal(candles.get(i), currentRsi, SignalType.BUY));
            } else if (previousRsi > overboughtLevel && currentRsi < overboughtLevel) {
                signals.add(buildSignal(candles.get(i), currentRsi, SignalType.SELL));
            }
        }

        return signals;
    }

    private RsiSignal buildSignal(OhlcCandleDto candle, double rsi, SignalType signalType) {
        return new RsiSignal(
                candle.getTime(),
                candle.getClose(),
                rsi,
                signalType
        );
    }

    private void validateParameters(int rsiPeriod, double oversoldLevel, double overboughtLevel) {
        if (rsiPeriod <= 0) {
            throw new IllegalArgumentException("RSI period must be greater than 0");
        }
        if (oversoldLevel <= 0 || overboughtLevel >= 100 || oversoldLevel >= overboughtLevel) {
            throw new IllegalArgumentException("RSI levels are invalid");
        }
    }
}
