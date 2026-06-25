package org.example.ingestion;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.CryptoCompareHttpClient;
import org.example.api.cryptocompare.dto.OhlcCandleDto;
import org.example.storage.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceIngestionApplication {

    private static final Logger log = LoggerFactory.getLogger(PriceIngestionApplication.class);

    private final CryptoCompareHttpClient cryptoCompareHttpClient;
    private final CandleRepository candleRepository;
    private final int candleLimit;

    public PriceIngestionApplication(
            CryptoCompareHttpClient cryptoCompareHttpClient,
            CandleRepository candleRepository,
            int candleLimit
    ) {
        if (cryptoCompareHttpClient == null) {
            throw new IllegalArgumentException("CryptoCompareHttpClient must not be null");
        }
        if (candleRepository == null) {
            throw new IllegalArgumentException("CandleRepository must not be null");
        }
        if (candleLimit <= 0) {
            throw new IllegalArgumentException("Candle limit must be greater than 0");
        }

        this.cryptoCompareHttpClient = cryptoCompareHttpClient;
        this.candleRepository = candleRepository;
        this.candleLimit = candleLimit;
    }

    public void runOnce(List<Crypto> assets) throws IOException, InterruptedException {
        if (assets == null || assets.isEmpty()) {
            throw new IllegalArgumentException("At least one crypto asset must be configured");
        }

        for (Crypto asset : assets) {
            List<OhlcCandleDto> candles = cryptoCompareHttpClient.getHourlyCandles(asset, candleLimit);
            candleRepository.saveHourlyCandles(asset, candles);
            log.info("Saved hourly candles. asset={}, candles={}", asset.getSymbol(), candles.size());
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
}
