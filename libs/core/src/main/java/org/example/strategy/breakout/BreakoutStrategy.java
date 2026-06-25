package org.example.strategy.breakout;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.SignalType;

@Getter
public class BreakoutStrategy {

    private static final int DEFAULT_LOOKBACK_PERIOD = 20;
    private static final int DEFAULT_BREAKOUT_COOLDOWN_CANDLES = 0;

    private final int lookbackPeriod;
    private final int breakoutCooldownCandles;

    public BreakoutStrategy() {
        this(DEFAULT_LOOKBACK_PERIOD, DEFAULT_BREAKOUT_COOLDOWN_CANDLES);
    }

    public BreakoutStrategy(int lookbackPeriod) {
        this(lookbackPeriod, DEFAULT_BREAKOUT_COOLDOWN_CANDLES);
    }

    public BreakoutStrategy(int lookbackPeriod, int breakoutCooldownCandles) {
        validateParameters(lookbackPeriod, breakoutCooldownCandles);
        this.lookbackPeriod = lookbackPeriod;
        this.breakoutCooldownCandles = breakoutCooldownCandles;
    }

    public List<BreakoutSignal> findSignals(List<OhlcCandleDto> candles) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() <= lookbackPeriod + 1) {
            return new ArrayList<>();
        }

        List<BreakoutSignal> signals = new ArrayList<>();
        int lastBuySignalIndex = -1;
        int lastSellSignalIndex = -1;

        for (int currentIndex = lookbackPeriod + 1; currentIndex < candles.size(); currentIndex++) {
            double currentHighestClose = findHighestClose(candles, currentIndex - lookbackPeriod, currentIndex);
            double currentLowestClose = findLowestClose(candles, currentIndex - lookbackPeriod, currentIndex);
            double previousHighestClose = findHighestClose(candles, currentIndex - lookbackPeriod - 1, currentIndex - 1);
            double previousLowestClose = findLowestClose(candles, currentIndex - lookbackPeriod - 1, currentIndex - 1);

            OhlcCandleDto currentCandle = candles.get(currentIndex);
            OhlcCandleDto previousCandle = candles.get(currentIndex - 1);
            double currentClose = currentCandle.getClose();
            double previousClose = previousCandle.getClose();

            boolean isBuyBreakout = currentClose > currentHighestClose && previousClose <= previousHighestClose;
            boolean isSellBreakout = currentClose < currentLowestClose && previousClose >= previousLowestClose;
            boolean isBuyCooldownActive = lastBuySignalIndex >= 0
                    && currentIndex - lastBuySignalIndex <= breakoutCooldownCandles;
            boolean isSellCooldownActive = lastSellSignalIndex >= 0
                    && currentIndex - lastSellSignalIndex <= breakoutCooldownCandles;

            if (isBuyBreakout && !isBuyCooldownActive) {
                signals.add(buildSignal(currentCandle, currentHighestClose, SignalType.BUY));
                lastBuySignalIndex = currentIndex;
            } else if (isSellBreakout && !isSellCooldownActive) {
                signals.add(buildSignal(currentCandle, currentLowestClose, SignalType.SELL));
                lastSellSignalIndex = currentIndex;
            }
        }

        return signals;
    }

    private double findHighestClose(List<OhlcCandleDto> candles, int fromInclusive, int toExclusive) {
        double highestClose = Double.NEGATIVE_INFINITY;
        for (int index = fromInclusive; index < toExclusive; index++) {
            highestClose = Math.max(highestClose, candles.get(index).getClose());
        }
        return highestClose;
    }

    private double findLowestClose(List<OhlcCandleDto> candles, int fromInclusive, int toExclusive) {
        double lowestClose = Double.POSITIVE_INFINITY;
        for (int index = fromInclusive; index < toExclusive; index++) {
            lowestClose = Math.min(lowestClose, candles.get(index).getClose());
        }
        return lowestClose;
    }

    private BreakoutSignal buildSignal(OhlcCandleDto candle, double breakoutLevel, SignalType signalType) {
        return new BreakoutSignal(
                candle.getTime(),
                candle.getClose(),
                breakoutLevel,
                lookbackPeriod,
                signalType
        );
    }

    private void validateParameters(int lookbackPeriod, int breakoutCooldownCandles) {
        if (lookbackPeriod <= 0) {
            throw new IllegalArgumentException("Lookback period must be greater than 0");
        }
        if (breakoutCooldownCandles < 0) {
            throw new IllegalArgumentException("Breakout cooldown candles must not be negative");
        }
    }
}
