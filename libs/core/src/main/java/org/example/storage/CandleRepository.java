package org.example.storage;

import java.io.IOException;
import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

public interface CandleRepository extends AutoCloseable {

    void saveHourlyCandles(Crypto asset, List<OhlcCandleDto> candles) throws IOException;

    List<OhlcCandleDto> loadHourlyCandles(Crypto asset, int limit) throws IOException;

    @Override
    default void close() {
    }
}
