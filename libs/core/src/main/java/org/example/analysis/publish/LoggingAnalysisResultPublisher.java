package org.example.analysis.publish;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.example.api.cryptocompare.Crypto;
import org.example.detector.DetectorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.example.strategy.StrategyResult;

public class LoggingAnalysisResultPublisher implements AnalysisResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAnalysisResultPublisher.class);

    @Override
    public void publishRunStarted(List<Crypto> assets, int candleLimit) {
        String assetSymbols = assets.stream()
                .map(Crypto::getSymbol)
                .collect(Collectors.joining(", "));
        withContext("run", null, () -> log.info(
                "Cycle started at {}. assets=[{}], candleLimit={}",
                Instant.now(),
                assetSymbols,
                candleLimit
        ));
    }

    @Override
    public void publishAssetStarted(Crypto asset, int candlesLoaded) {
        withContext("asset-start", asset.getSymbol(), () ->
                log.info("Asset analysis started. asset={}, candlesLoaded={}", asset.getSymbol(), candlesLoaded));
    }

    @Override
    public void publishStrategyResult(StrategyResult result) {
        withComponentContext("strategy", result.asset().getSymbol(), result.strategyName(), () -> {
            log.info(
                    "Strategy completed. strategy={}, asset={}, candlesAnalyzed={}, signalCount={}",
                    result.strategyName(),
                    result.asset().getSymbol(),
                    result.candlesAnalyzed(),
                    result.signalCount()
            );

            for (String signalDescription : result.signalDescriptions()) {
                log.info("Strategy signal: {}", signalDescription);
            }
        });
    }

    @Override
    public void publishDetectorResult(DetectorResult<?> result) {
        withComponentContext("detector", result.asset().getSymbol(), result.detectorName(), () -> {
            log.info(
                    "Detector completed. detector={}, asset={}, candlesAnalyzed={}, detectionCount={}",
                    result.detectorName(),
                    result.asset().getSymbol(),
                    result.candlesAnalyzed(),
                    result.detectionCount()
            );
        });
    }

    @Override
    public void publishAssetCompleted(Crypto asset) {
        withContext("asset-complete", asset.getSymbol(), () ->
                log.info("Asset analysis completed. asset={}", asset.getSymbol()));
    }

    @Override
    public void publishAssetFailed(Crypto asset, Exception exception) {
        withContext("asset-failed", asset.getSymbol(), () ->
                log.error("Failed to analyze asset={}", asset.getSymbol(), exception));
    }

    @Override
    public void publishRunCompleted() {
        withContext("run", null, () -> log.info("Cycle finished at {}", Instant.now()));
    }

    private void withComponentContext(String phase, String asset, String component, Runnable action) {
        String previousComponent = MDC.get("component");
        MDC.put("component", component);
        try {
            withContext(phase, asset, action);
        } finally {
            restore("component", previousComponent);
        }
    }

    private void withContext(String phase, String asset, Runnable action) {
        String previousPhase = MDC.get("phase");
        String previousAsset = MDC.get("asset");
        putOrRemove("phase", phase);
        putOrRemove("asset", asset);

        try {
            action.run();
        } finally {
            restore("phase", previousPhase);
            restore("asset", previousAsset);
        }
    }

    private void putOrRemove(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    private void restore(String key, String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previousValue);
    }
}
