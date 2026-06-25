package org.example.backtester;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.example.analysis.publish.AnalysisResultPublisher;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.detector.DetectorResult;
import org.example.detector.MarketDetector;
import org.example.storage.CandleRepository;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.StrategyResult;

public class StrategyBacktestApplication {

    private final CandleRepository candleRepository;
    private final int candleLimit;
    private final List<AnalysisStrategy> strategies;
    private final List<MarketDetector<?>> detectors;
    private final AnalysisResultPublisher resultPublisher;

    public StrategyBacktestApplication(
            CandleRepository candleRepository,
            int candleLimit,
            List<AnalysisStrategy> strategies,
            List<MarketDetector<?>> detectors,
            AnalysisResultPublisher resultPublisher
    ) {
        if (candleRepository == null) {
            throw new IllegalArgumentException("CandleRepository must not be null");
        }
        if (candleLimit <= 0) {
            throw new IllegalArgumentException("Candle limit must be greater than 0");
        }
        if (resultPublisher == null) {
            throw new IllegalArgumentException("AnalysisResultPublisher must not be null");
        }

        this.candleRepository = candleRepository;
        this.candleLimit = candleLimit;
        this.strategies = strategies == null ? List.of() : List.copyOf(strategies);
        this.detectors = detectors == null ? List.of() : List.copyOf(detectors);
        this.resultPublisher = resultPublisher;
        if (this.strategies.isEmpty() && this.detectors.isEmpty()) {
            throw new IllegalArgumentException("At least one strategy or detector must be configured");
        }
    }

    public void runOnce(List<Crypto> assets) throws IOException {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("At least one crypto asset must be configured");
        }

        resultPublisher.publishRunStarted(assets, candleLimit);
        try {
            for (Crypto asset : assets) {
                try {
                    backtestAsset(asset);
                } catch (IOException | RuntimeException e) {
                    resultPublisher.publishAssetFailed(asset, e);
                }
            }
        } finally {
            resultPublisher.publishRunCompleted();
        }
    }

    public void runInLoop(List<Crypto> assets, Duration pauseBetweenCycles) throws IOException, InterruptedException {
        if (pauseBetweenCycles == null || pauseBetweenCycles.isNegative() || pauseBetweenCycles.isZero()) {
            throw new IllegalArgumentException("Pause between cycles must be greater than 0");
        }

        while (!Thread.currentThread().isInterrupted()) {
            runOnce(assets);
            Thread.sleep(pauseBetweenCycles.toMillis());
        }
    }

    private void backtestAsset(Crypto asset) throws IOException {
        List<OhlcCandleDto> candles = candleRepository.loadHourlyCandles(asset, candleLimit);
        resultPublisher.publishAssetStarted(asset, candles.size());

        for (AnalysisStrategy strategy : strategies) {
            StrategyResult result = strategy.analyze(asset, candles);
            resultPublisher.publishStrategyResult(result);
        }

        for (MarketDetector<?> detector : detectors) {
            DetectorResult<?> result = detector.detect(asset, candles);
            resultPublisher.publishDetectorResult(result);
        }

        resultPublisher.publishAssetCompleted(asset);
    }
}
