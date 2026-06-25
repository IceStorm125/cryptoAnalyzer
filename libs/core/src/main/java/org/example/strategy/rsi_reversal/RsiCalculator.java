package org.example.strategy.rsi_reversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RsiCalculator {

    public static List<Double> calculate(List<Double> closePrices, int period) {
        validateInput(closePrices, period);

        List<Double> rsiValues = new ArrayList<>(Collections.nCopies(closePrices.size(), null));

        double gainSum = 0.0;
        double lossSum = 0.0;
        for (int i = 1; i <= period; i++) {
            double change = closePrices.get(i) - closePrices.get(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double averageGain = gainSum / period;
        double averageLoss = lossSum / period;
        rsiValues.set(period, calculateRsi(averageGain, averageLoss));

        for (int i = period + 1; i < closePrices.size(); i++) {
            double change = closePrices.get(i) - closePrices.get(i - 1);
            double currentGain = Math.max(change, 0.0);
            double currentLoss = Math.max(-change, 0.0);

            averageGain = ((averageGain * (period - 1)) + currentGain) / period;
            averageLoss = ((averageLoss * (period - 1)) + currentLoss) / period;

            rsiValues.set(i, calculateRsi(averageGain, averageLoss));
        }

        return rsiValues;
    }

    public static List<Double> calculateFromCandles(List<OhlcCandleDto> candles, int period) {
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }

        List<Double> closePrices = new ArrayList<>(candles.size());
        for (OhlcCandleDto candle : candles) {
            closePrices.add(candle.getClose());
        }

        return calculate(closePrices, period);
    }

    private static double calculateRsi(double averageGain, double averageLoss) {
        if (averageLoss == 0.0) {
            return 100.0;
        }

        double relativeStrength = averageGain / averageLoss;
        return 100.0 - (100.0 / (1.0 + relativeStrength));
    }

    private static void validateInput(List<Double> closePrices, int period) {
        if (closePrices == null) {
            throw new IllegalArgumentException("Close prices must not be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be greater than 0");
        }
        if (closePrices.size() <= period) {
            throw new IllegalArgumentException("Close prices size must be greater than period");
        }
    }
}
