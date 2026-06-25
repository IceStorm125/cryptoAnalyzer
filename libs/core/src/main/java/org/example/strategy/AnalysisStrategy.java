package org.example.strategy;

import java.util.List;
import org.example.api.cryptocompare.Crypto;
import org.example.api.cryptocompare.dto.OhlcCandleDto;

public interface AnalysisStrategy {

    String getName();

    StrategyResult analyze(Crypto asset, List<OhlcCandleDto> candles);
}
