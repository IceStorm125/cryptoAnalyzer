package org.example.ingestion;

import java.io.IOException;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.CryptoCompareHttpClient;
import org.example.config.ApplicationSettings;
import org.example.config.ApplicationSettingsLoader;
import org.example.config.LoggingSupport;
import org.example.storage.ClickHouseCandleRepository;

public class PriceIngestionMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationSettings settings = ApplicationSettingsLoader.load();
        LoggingSupport.configure("price-ingestion", settings);

        List<Crypto> assets = List.of(
                Crypto.BTC,
                Crypto.ETH,
                Crypto.SOL
        );

        try (ClickHouseCandleRepository candleRepository = new ClickHouseCandleRepository(settings.clickHouse())) {
            PriceIngestionApplication application = new PriceIngestionApplication(
                    new CryptoCompareHttpClient(),
                    candleRepository,
                    settings.candleLimit()
            );

            if (settings.runInLoop()) {
                application.runInLoop(assets, settings.pauseBetweenCycles());
            } else {
                application.runOnce(assets);
            }
        }
    }
}
