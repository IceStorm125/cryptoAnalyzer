package org.example.detector;

import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

public interface MarketDetector<T> {

    String getName();

    DetectorResult<T> detect(Crypto asset, List<OhlcCandleDto> candles);
}
