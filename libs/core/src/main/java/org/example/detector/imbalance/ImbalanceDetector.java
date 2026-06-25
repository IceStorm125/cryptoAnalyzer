package org.example.detector.imbalance;

import java.util.ArrayList;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.detector.DetectorResult;
import org.example.detector.MarketDetector;

public class ImbalanceDetector implements MarketDetector<Imbalance> {

    private static final String DETECTOR_NAME = "Imbalance detector";

    @Override
    public String getName() {
        return DETECTOR_NAME;
    }

    @Override
    public DetectorResult<Imbalance> detect(Crypto asset, List<OhlcCandleDto> candles) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() < 3) {
            return new DetectorResult<>(getName(), asset, candles.size(), List.of());
        }

        List<Imbalance> untestedImbalances = new ArrayList<>();
        Integer lastAcceptedBullishIndex = null;
        Integer lastAcceptedBearishIndex = null;

        for (int currentIndex = 2; currentIndex < candles.size(); currentIndex++) {
            OhlcCandleDto firstCandle = candles.get(currentIndex - 2);
            OhlcCandleDto middleCandle = candles.get(currentIndex - 1);
            OhlcCandleDto thirdCandle = candles.get(currentIndex);

            Imbalance bullishImbalance = ImbalanceLogic.buildBullishImbalance(firstCandle, middleCandle, thirdCandle);
            if (bullishImbalance != null
                    && isUntested(candles, currentIndex + 1, bullishImbalance)
                    && !isOverlappingWithPreviousWindow(currentIndex, lastAcceptedBullishIndex)) {
                untestedImbalances.add(bullishImbalance);
                lastAcceptedBullishIndex = currentIndex;
            }

            Imbalance bearishImbalance = ImbalanceLogic.buildBearishImbalance(firstCandle, middleCandle, thirdCandle);
            if (bearishImbalance != null
                    && isUntested(candles, currentIndex + 1, bearishImbalance)
                    && !isOverlappingWithPreviousWindow(currentIndex, lastAcceptedBearishIndex)) {
                untestedImbalances.add(bearishImbalance);
                lastAcceptedBearishIndex = currentIndex;
            }
        }

        return new DetectorResult<>(getName(), asset, candles.size(), untestedImbalances);
    }

    private boolean isOverlappingWithPreviousWindow(int currentIndex, Integer lastAcceptedIndex) {
        if (lastAcceptedIndex == null) {
            return false;
        }

        return currentIndex - lastAcceptedIndex == 1;
    }

    private boolean isUntested(List<OhlcCandleDto> candles, int startIndex, Imbalance imbalance) {
        for (int index = startIndex; index < candles.size(); index++) {
            OhlcCandleDto candle = candles.get(index);
            if (intersectsImbalance(candle, imbalance)) {
                return false;
            }
        }

        return true;
    }

    private boolean intersectsImbalance(OhlcCandleDto candle, Imbalance imbalance) {
        return candle.getLow() < imbalance.upperBound()
                && candle.getHigh() > imbalance.lowerBound();
    }
}
