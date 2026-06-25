package org.example.analysis.publish;

import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.detector.DetectorResult;
import org.example.strategy.StrategyResult;

public class CompositeAnalysisResultPublisher implements AnalysisResultPublisher {

    private final List<AnalysisResultPublisher> delegates;

    public CompositeAnalysisResultPublisher(List<AnalysisResultPublisher> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("At least one publisher must be configured");
        }
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void publishRunStarted(List<Crypto> assets, int candleLimit) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishRunStarted(assets, candleLimit);
        }
    }

    @Override
    public void publishRunCompleted() {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishRunCompleted();
        }
    }

    @Override
    public void publishAssetStarted(Crypto asset, int candlesLoaded) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishAssetStarted(asset, candlesLoaded);
        }
    }

    @Override
    public void publishStrategyResult(StrategyResult result) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishStrategyResult(result);
        }
    }

    @Override
    public void publishDetectorResult(DetectorResult<?> result) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishDetectorResult(result);
        }
    }

    @Override
    public void publishAssetCompleted(Crypto asset) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishAssetCompleted(asset);
        }
    }

    @Override
    public void publishAssetFailed(Crypto asset, Exception exception) {
        for (AnalysisResultPublisher delegate : delegates) {
            delegate.publishAssetFailed(asset, exception);
        }
    }

    @Override
    public void close() {
        RuntimeException firstFailure = null;

        for (int i = delegates.size() - 1; i >= 0; i--) {
            try {
                delegates.get(i).close();
            } catch (RuntimeException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
    }
}
