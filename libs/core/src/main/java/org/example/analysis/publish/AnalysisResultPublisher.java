package org.example.analysis.publish;

import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.detector.DetectorResult;
import org.example.strategy.StrategyResult;

public interface AnalysisResultPublisher extends AutoCloseable {

    default void publishRunStarted(List<Crypto> assets, int candleLimit) {
    }

    default void publishRunCompleted() {
    }

    void publishAssetStarted(Crypto asset, int candlesLoaded);

    void publishStrategyResult(StrategyResult result);

    void publishDetectorResult(DetectorResult<?> result);

    default void publishAssetCompleted(Crypto asset) {
    }

    void publishAssetFailed(Crypto asset, Exception exception);

    @Override
    default void close() {
    }
}
