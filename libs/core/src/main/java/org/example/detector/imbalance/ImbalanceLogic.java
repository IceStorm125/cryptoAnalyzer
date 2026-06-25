package org.example.detector.imbalance;

import org.example.api.cryptocompare.dto.OhlcCandleDto;

public final class ImbalanceLogic {

    private static final double MIN_RELATIVE_IMBALANCE_SIZE = 0.0005;
    private static final double MIN_BODY_TO_RANGE_RATIO = 0.5;

    private ImbalanceLogic() {
    }

    public static Imbalance buildBullishImbalance(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        if (!hasBullishDisplacement(firstCandle, middleCandle, thirdCandle)) {
            return null;
        }

        double wickGapLowerBound = firstCandle.getHigh();
        double wickGapUpperBound = thirdCandle.getLow();
        if (isValidZone(wickGapLowerBound, wickGapUpperBound, thirdCandle.getClose())) {
            return new Imbalance(
                    thirdCandle.getTime(),
                    wickGapLowerBound,
                    wickGapUpperBound,
                    ImbalanceType.BULLISH
            );
        }

        double firstBodyHigh = Math.max(firstCandle.getOpen(), firstCandle.getClose());
        double thirdBodyLow = Math.min(thirdCandle.getOpen(), thirdCandle.getClose());
        if (isValidZone(firstBodyHigh, thirdBodyLow, thirdCandle.getClose())) {
            return new Imbalance(
                    thirdCandle.getTime(),
                    firstBodyHigh,
                    thirdBodyLow,
                    ImbalanceType.BULLISH
            );
        }

        return null;
    }

    public static Imbalance buildBearishImbalance(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        if (!hasBearishDisplacement(firstCandle, middleCandle, thirdCandle)) {
            return null;
        }

        double wickGapLowerBound = thirdCandle.getHigh();
        double wickGapUpperBound = firstCandle.getLow();
        if (isValidZone(wickGapLowerBound, wickGapUpperBound, thirdCandle.getClose())) {
            return new Imbalance(
                    thirdCandle.getTime(),
                    wickGapLowerBound,
                    wickGapUpperBound,
                    ImbalanceType.BEARISH
            );
        }

        double thirdBodyHigh = Math.max(thirdCandle.getOpen(), thirdCandle.getClose());
        double firstBodyLow = Math.min(firstCandle.getOpen(), firstCandle.getClose());
        if (isValidZone(thirdBodyHigh, firstBodyLow, thirdCandle.getClose())) {
            return new Imbalance(
                    thirdCandle.getTime(),
                    thirdBodyHigh,
                    firstBodyLow,
                    ImbalanceType.BEARISH
            );
        }

        return null;
    }

    private static boolean hasBullishDisplacement(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        return isBullishCandle(middleCandle)
                && bodyToRangeRatio(middleCandle) >= MIN_BODY_TO_RANGE_RATIO
                && thirdCandle.getClose() > firstCandle.getHigh();
    }

    private static boolean hasBearishDisplacement(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        return isBearishCandle(middleCandle)
                && bodyToRangeRatio(middleCandle) >= MIN_BODY_TO_RANGE_RATIO
                && thirdCandle.getClose() < firstCandle.getLow();
    }

    private static boolean isValidZone(double lowerBound, double upperBound, double referencePrice) {
        if (upperBound <= lowerBound) {
            return false;
        }

        return (upperBound - lowerBound) / Math.max(Math.abs(referencePrice), 1.0) >= MIN_RELATIVE_IMBALANCE_SIZE;
    }

    private static double bodyToRangeRatio(OhlcCandleDto candle) {
        double range = candle.getHigh() - candle.getLow();
        if (range <= 0) {
            return 0;
        }

        double body = Math.abs(candle.getClose() - candle.getOpen());
        return body / range;
    }

    private static boolean isBullishCandle(OhlcCandleDto candle) {
        return candle.getClose() > candle.getOpen();
    }

    private static boolean isBearishCandle(OhlcCandleDto candle) {
        return candle.getClose() < candle.getOpen();
    }
}
