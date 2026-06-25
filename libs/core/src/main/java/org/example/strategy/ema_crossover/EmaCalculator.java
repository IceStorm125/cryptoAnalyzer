package org.example.strategy.ema_crossover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmaCalculator {

    public static List<Double> calculate(List<Double> closePrices, int period) {
        validateInput(closePrices, period);

        List<Double> emaValues = new ArrayList<>(Collections.nCopies(closePrices.size(), null));
        double multiplier = 2.0 / (period + 1.0);

        double seed = 0.0;
        for (int i = 0; i < period; i++) {
            seed += closePrices.get(i);
        }

        double previousEma = seed / period;
        emaValues.set(period - 1, previousEma);

        for (int i = period; i < closePrices.size(); i++) {
            double currentClose = closePrices.get(i);
            double currentEma = ((currentClose - previousEma) * multiplier) + previousEma;
            emaValues.set(i, currentEma);
            previousEma = currentEma;
        }

        return emaValues;
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

    private static void validateInput(List<Double> closePrices, int period) {
        if (closePrices == null) {
            throw new IllegalArgumentException("Close prices must not be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be greater than 0");
        }
        if (closePrices.size() < period) {
            throw new IllegalArgumentException("Close prices size must be greater than or equal to period");
        }
    }
}
