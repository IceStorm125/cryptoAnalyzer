package org.example.detector.order_block;

import java.util.ArrayList;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.detector.DetectorResult;
import org.example.detector.MarketDetector;
import org.example.detector.imbalance.Imbalance;
import org.example.detector.imbalance.ImbalanceLogic;

public class OrderBlockDetector implements MarketDetector<OrderBlock> {

    private static final String DETECTOR_NAME = "Order block detector";

    @Override
    public String getName() {
        return DETECTOR_NAME;
    }

    @Override
    public DetectorResult<OrderBlock> detect(Crypto asset, List<OhlcCandleDto> candles) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null");
        }
        if (candles == null) {
            throw new IllegalArgumentException("Candles must not be null");
        }
        if (candles.size() < 3) {
            return new DetectorResult<>(getName(), asset, candles.size(), List.of());
        }

        List<OrderBlock> untestedOrderBlocks = new ArrayList<>();

        for (int currentIndex = 2; currentIndex < candles.size(); currentIndex++) {
            OhlcCandleDto firstCandle = candles.get(currentIndex - 2);
            OhlcCandleDto middleCandle = candles.get(currentIndex - 1);
            OhlcCandleDto thirdCandle = candles.get(currentIndex);

            OrderBlock bullishOrderBlock = buildBullishOrderBlock(firstCandle, middleCandle, thirdCandle);
            if (bullishOrderBlock != null && isUntested(candles, currentIndex + 1, bullishOrderBlock)) {
                untestedOrderBlocks.add(bullishOrderBlock);
            }

            OrderBlock bearishOrderBlock = buildBearishOrderBlock(firstCandle, middleCandle, thirdCandle);
            if (bearishOrderBlock != null && isUntested(candles, currentIndex + 1, bearishOrderBlock)) {
                untestedOrderBlocks.add(bearishOrderBlock);
            }
        }

        return new DetectorResult<>(getName(), asset, candles.size(), untestedOrderBlocks);
    }

    private OrderBlock buildBullishOrderBlock(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        Imbalance imbalance = ImbalanceLogic.buildBullishImbalance(firstCandle, middleCandle, thirdCandle);
        boolean middleCandleIsBearish = middleCandle.getClose() < middleCandle.getOpen();

        if (imbalance == null || !middleCandleIsBearish) {
            return null;
        }

        return new OrderBlock(
                middleCandle.getTime(),
                middleCandle.getLow(),
                middleCandle.getHigh(),
                OrderBlockType.BULLISH
        );
    }

    private OrderBlock buildBearishOrderBlock(
            OhlcCandleDto firstCandle,
            OhlcCandleDto middleCandle,
            OhlcCandleDto thirdCandle
    ) {
        Imbalance imbalance = ImbalanceLogic.buildBearishImbalance(firstCandle, middleCandle, thirdCandle);
        boolean middleCandleIsBullish = middleCandle.getClose() > middleCandle.getOpen();

        if (imbalance == null || !middleCandleIsBullish) {
            return null;
        }

        return new OrderBlock(
                middleCandle.getTime(),
                middleCandle.getLow(),
                middleCandle.getHigh(),
                OrderBlockType.BEARISH
        );
    }

    private boolean isUntested(List<OhlcCandleDto> candles, int startIndex, OrderBlock orderBlock) {
        for (int index = startIndex; index < candles.size(); index++) {
            OhlcCandleDto candle = candles.get(index);
            if (intersectsOrderBlock(candle, orderBlock)) {
                return false;
            }
        }

        return true;
    }

    private boolean intersectsOrderBlock(OhlcCandleDto candle, OrderBlock orderBlock) {
        return candle.getLow() < orderBlock.upperBound()
                && candle.getHigh() > orderBlock.lowerBound();
    }
}
