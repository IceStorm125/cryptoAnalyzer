package org.example.backtester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.example.analysis.publish.AnalysisResultPublisher;
import org.example.analysis.publish.CompositeAnalysisResultPublisher;
import org.example.analysis.publish.LoggingAnalysisResultPublisher;
import org.example.api.cryptocompare.Crypto;
import org.example.config.ApplicationSettings;
import org.example.config.ApplicationSettingsLoader;
import org.example.config.LoggingSupport;
import org.example.detector.MarketDetector;
import org.example.redis.RedisAnalysisResultPublisher;
import org.example.redis.RedisPublisherSettings;
import org.example.storage.ClickHouseCandleRepository;
import org.example.strategy.AnalysisStrategy;
import org.example.strategy.ema_crossover.EmaCrossoverAnalysisStrategy;
import org.example.strategy.ema_crossover.EmaCrossoverStrategySettings;

public class StrategyBacktesterMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationSettings settings = ApplicationSettingsLoader.load();
        LoggingSupport.configure("strategy-backtester", settings);

        List<Crypto> assets = List.of(
                Crypto.BTC,
                Crypto.ETH,
                Crypto.SOL
        );

        List<AnalysisStrategy> enabledStrategies = List.of(
                new EmaCrossoverAnalysisStrategy(new EmaCrossoverStrategySettings(50, 200, 0.5))
        );

        List<MarketDetector<?>> enabledDetectors = List.of();

        try (
                ClickHouseCandleRepository candleRepository = new ClickHouseCandleRepository(settings.clickHouse());
                AnalysisResultPublisher resultPublisher = buildResultPublisher(settings)
        ) {
            StrategyBacktestApplication application = new StrategyBacktestApplication(
                    candleRepository,
                    settings.candleLimit(),
                    enabledStrategies,
                    enabledDetectors,
                    resultPublisher
            );

            if (settings.runInLoop()) {
                application.runInLoop(assets, settings.pauseBetweenCycles());
            } else {
                application.runOnce(assets);
            }
        }
    }

    private static AnalysisResultPublisher buildResultPublisher(ApplicationSettings settings) {
        List<AnalysisResultPublisher> publishers = new ArrayList<>();
        publishers.add(new LoggingAnalysisResultPublisher());

        if (settings.redis().enabled()) {
            publishers.add(new RedisAnalysisResultPublisher(new RedisPublisherSettings(
                    settings.redis().host(),
                    settings.redis().port(),
                    settings.redis().database(),
                    settings.redis().username(),
                    settings.redis().password(),
                    settings.redis().keyPrefix(),
                    settings.redis().historyMaxLen()
            )));
        }

        return new CompositeAnalysisResultPublisher(publishers);
    }
}
