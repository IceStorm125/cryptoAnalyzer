package org.example.strategy.ema_crossover;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.strategy.SignalType;

@Getter
public class EmaCrossoverStrategy {

    private static final int DEFAULT_FAST_PERIOD = 50;
    private static final int DEFAULT_SLOW_PERIOD = 100;
    private static final double DEFAULT_RETEST_TOLERANCE_PERCENT = 0.5;
    private static final int DEFAULT_RETEST_WINDOW_CANDLES = Integer.MAX_VALUE;

    private final int fastPeriod;
    private final int slowPeriod;
    private final double retestTolerancePercent;
    private final int retestWindowCandles;

    public EmaCrossoverStrategy() {
        this(DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_RETEST_TOLERANCE_PERCENT, DEFAULT_RETEST_WINDOW_CANDLES);
    }

    public EmaCrossoverStrategy(int fastPeriod, int slowPeriod) {
        this(fastPeriod, slowPeriod, DEFAULT_RETEST_TOLERANCE_PERCENT, DEFAULT_RETEST_WINDOW_CANDLES);
    }

    public EmaCrossoverStrategy(int fastPeriod, int slowPeriod, double retestTolerancePercent) {
        this(fastPeriod, slowPeriod, retestTolerancePercent, DEFAULT_RETEST_WINDOW_CANDLES);
    }

    public EmaCrossoverStrategy(
            int fastPeriod,
            int slowPeriod,
            double retestTolerancePercent,
            int retestWindowCandles
    ) {
        validateParameters(fastPeriod, slowPeriod, retestTolerancePercent, retestWindowCandles);
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.retestTolerancePercent = retestTolerancePercent;
        this.retestWindowCandles = retestWindowCandles;
    }

    public List<EmaCrossoverSignal> findSignals(List<OhlcCandleDto> candles) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() < slowPeriod) {
            return new ArrayList<>();
        }

        List<Double> fastEmaValues = EmaCalculator.calculateFromCandles(candles, fastPeriod);
        List<Double> slowEmaValues = EmaCalculator.calculateFromCandles(candles, slowPeriod);
        List<EmaCrossoverSignal> signals = new ArrayList<>();
        SignalType activeCrossoverType = null;
        int crossoverIndex = -1;
        boolean previousCandleWasInRetestZone = false;

        for (int i = 1; i < candles.size(); i++) {
            Double previousFastEma = fastEmaValues.get(i - 1);
            Double previousSlowEma = slowEmaValues.get(i - 1);
            Double currentFastEma = fastEmaValues.get(i);
            Double currentSlowEma = slowEmaValues.get(i);

            if (previousFastEma == null || previousSlowEma == null || currentFastEma == null || currentSlowEma == null) {
                continue;
            }

            if (previousFastEma <= previousSlowEma && currentFastEma > currentSlowEma) {
                activeCrossoverType = SignalType.BUY;
                crossoverIndex = i;
                previousCandleWasInRetestZone = false;
            } else if (previousFastEma >= previousSlowEma && currentFastEma < currentSlowEma) {
                activeCrossoverType = SignalType.SELL;
                crossoverIndex = i;
                previousCandleWasInRetestZone = false;
            } else if (activeCrossoverType != null) {
                int candlesAfterCrossover = i - crossoverIndex;
                if (candlesAfterCrossover > retestWindowCandles) {
                    previousCandleWasInRetestZone = false;
                    continue;
                }

                OhlcCandleDto candle = candles.get(i);
                boolean currentCandleIsInRetestZone = isFastEmaRetest(
                        candle,
                        currentFastEma,
                        currentSlowEma,
                        activeCrossoverType
                );

                if (currentCandleIsInRetestZone && !previousCandleWasInRetestZone) {
                    signals.add(buildSignal(
                            candle,
                            currentFastEma,
                            currentSlowEma,
                            activeCrossoverType,
                            candlesAfterCrossover
                    ));
                }

                previousCandleWasInRetestZone = currentCandleIsInRetestZone;
            }
        }

        return signals;
    }

    private boolean isFastEmaRetest(
            OhlcCandleDto candle,
            double fastEma,
            double slowEma,
            SignalType signalType
    ) {
        double tolerance = fastEma * retestTolerancePercent / 100.0;

        if (signalType == SignalType.BUY) {
            boolean trendStillValid = fastEma > slowEma;
            boolean touchedFastEma = candleIntersectsFastEmaZone(candle, fastEma, tolerance);
            return trendStillValid && touchedFastEma;
        }

        boolean trendStillValid = fastEma < slowEma;
        boolean touchedFastEma = candleIntersectsFastEmaZone(candle, fastEma, tolerance);
        return trendStillValid && touchedFastEma;
    }

    private boolean candleIntersectsFastEmaZone(OhlcCandleDto candle, double fastEma, double tolerance) {
        double lowerZoneBound = fastEma - tolerance;
        double upperZoneBound = fastEma + tolerance;
        return candle.getLow() <= upperZoneBound && candle.getHigh() >= lowerZoneBound;
    }

    private EmaCrossoverSignal buildSignal(
            OhlcCandleDto candle,
            double fastEma,
            double slowEma,
            SignalType signalType,
            int candlesAfterCrossover
    ) {
        return new EmaCrossoverSignal(
                candle.getTime(),
                candle.getClose(),
                fastEma,
                slowEma,
                calculateRetestDistancePercent(candle, fastEma),
                candlesAfterCrossover,
                signalType
        );
    }

    private double calculateRetestDistancePercent(OhlcCandleDto candle, double fastEma) {
        if (fastEma == 0) {
            return 0;
        }

        if (candle.getLow() <= fastEma && candle.getHigh() >= fastEma) {
            return 0;
        }

        double nearestPrice = candle.getHigh() < fastEma ? candle.getHigh() : candle.getLow();
        return Math.abs(nearestPrice - fastEma) / fastEma * 100.0;
    }

    private void validateParameters(
            int fastPeriod,
            int slowPeriod,
            double retestTolerancePercent,
            int retestWindowCandles
    ) {
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
