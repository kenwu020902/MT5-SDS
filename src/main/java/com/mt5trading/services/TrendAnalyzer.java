package com.mt5trading.services;

import com.mt5trading.config.TradingConfig;
import com.mt5trading.models.CandleData;
import com.mt5trading.models.MACDData;
import com.mt5trading.models.TrendDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrendAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(TrendAnalyzer.class);
    
    private final TradingConfig config;
    private final MACDCalculator macdCalculator;
    
    public TrendAnalyzer(TradingConfig config) {
        this.config = config;
        this.macdCalculator = new MACDCalculator(
                config.getMacdFastPeriod(),
                config.getMacdSlowPeriod(),
                config.getMacdSignalPeriod()
        );
    }
    
    public TrendDirection analyzeTrend(List<CandleData> candles) {
        if (candles == null || candles.size() < 2) {
            logger.warn("Not enough candles for trend analysis");
            return TrendDirection.NONE;
        }
        
        CandleData previousCandle = candles.get(candles.size() - 2);
        CandleData currentCandle = candles.get(candles.size() - 1);
        
        return confirmTrend(previousCandle, currentCandle, candles);
    }
    
    public TrendDirection confirmTrend(CandleData previousCandle, CandleData currentCandle, 
                                      List<CandleData> candles) {
        if (previousCandle == null || currentCandle == null) {
            return TrendDirection.NONE;
        }
        
        // Check MACD confirmation if enabled
        MACDData macdData = null;
        if (config.isUseMACDConfirmation()) {
            macdData = macdCalculator.calculateCurrentMACD(candles);
        }
        
        // Bullish confirmation
        if (previousCandle.isBullish()) {
            boolean candleConfirmation = config.isUseStrictConfirmation() 
                    ? (currentCandle.getOpen() > previousCandle.getHigh())
                    : (currentCandle.getOpen() > previousCandle.getClose());
            
            boolean macdConfirmation = !config.isUseMACDConfirmation() 
                    || (macdData != null && macdData.isBullish());
            
            if (candleConfirmation && macdConfirmation) {
                logger.info("Uptrend confirmed. Previous bullish candle, new candle opens above {}",
                           config.isUseStrictConfirmation() ? "previous high" : "previous close");
                return TrendDirection.UPTREND;
            }
        }
        
        // Bearish confirmation
        if (previousCandle.isBearish()) {
            boolean candleConfirmation = config.isUseStrictConfirmation()
                    ? (currentCandle.getOpen() < previousCandle.getLow())
                    : (currentCandle.getOpen() < previousCandle.getClose());
            
            boolean macdConfirmation = !config.isUseMACDConfirmation()
                    || (macdData != null && macdData.isBearish());
            
            if (candleConfirmation && macdConfirmation) {
                logger.info("Downtrend confirmed. Previous bearish candle, new candle opens below {}",
                           config.isUseStrictConfirmation() ? "previous low" : "previous close");
                return TrendDirection.DOWNTREND;
            }
        }
        
        return TrendDirection.NONE;
    }
    
    public double calculateTrendStrength(List<CandleData> candles) {
        if (candles == null || candles.size() < 10) {
            return 0.0;
        }
        
        int bullishCount = 0;
        int totalCount = Math.min(candles.size(), 20); // Last 20 candles
        
        for (int i = candles.size() - totalCount; i < candles.size(); i++) {
            if (candles.get(i).isBullish()) {
                bullishCount++;
            }
        }
        
        double ratio = (double) bullishCount / totalCount;
        
        // Convert to -1 (strong bearish) to +1 (strong bullish)
        return (ratio - 0.5) * 2;
    }
    
    public boolean isMarketStructureBullish(List<CandleData> candles) {
        if (candles.size() < 5) return false;
        
        // Check for higher highs and higher lows
        int checkCount = Math.min(candles.size() - 1, 5);
        
        for (int i = 0; i < checkCount; i++) {
            int currentIndex = candles.size() - 1 - i;
            int prevIndex = currentIndex - 1;
            
            if (candles.get(currentIndex).getHigh() < candles.get(prevIndex).getHigh()) {
                return false;
            }
            if (candles.get(currentIndex).getLow() < candles.get(prevIndex).getLow()) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isMarketStructureBearish(List<CandleData> candles) {
        if (candles.size() < 5) return false;
        
        // Check for lower highs and lower lows
        int checkCount = Math.min(candles.size() - 1, 5);
        
        for (int i = 0; i < checkCount; i++) {
            int currentIndex = candles.size() - 1 - i;
            int prevIndex = currentIndex - 1;
            
            if (candles.get(currentIndex).getHigh() > candles.get(prevIndex).getHigh()) {
                return false;
            }
            if (candles.get(currentIndex).getLow() > candles.get(prevIndex).getLow()) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean hasVolatilityExpansion(CandleData previousCandle, CandleData currentCandle) {
        if (previousCandle == null || currentCandle == null) return false;
        
        double previousRange = previousCandle.getTotalRange();
        double currentRange = currentCandle.getTotalRange();
        
        // Current range is at least 1.5 times previous range
        return currentRange > previousRange * 1.5;
    }
}