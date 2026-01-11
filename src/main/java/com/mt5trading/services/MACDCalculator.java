package com.mt5trading.services;

import com.mt5trading.models.CandleData;
import com.mt5trading.models.MACDData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MACDCalculator {
    private static final Logger logger = LoggerFactory.getLogger(MACDCalculator.class);
    
    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;
    
    public MACDCalculator(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }
    
    public MACDData calculateMACD(List<CandleData> candles, int index) {
        if (candles == null || candles.size() < slowPeriod + signalPeriod) {
            logger.warn("Not enough data to calculate MACD. Required: {}, Available: {}", 
                       slowPeriod + signalPeriod, candles == null ? 0 : candles.size());
            return null;
        }
        
        try {
            // Calculate EMA for fast period
            double fastEMA = calculateEMA(candles, index, fastPeriod);
            
            // Calculate EMA for slow period
            double slowEMA = calculateEMA(candles, index, slowPeriod);
            
            // Calculate MACD line
            double macdLine = fastEMA - slowEMA;
            
            // Calculate signal line (EMA of MACD line)
            double signalLine = calculateSignalLine(candles, index, macdLine);
            
            // Calculate histogram
            double histogram = macdLine - signalLine;
            
            return new MACDData(macdLine, signalLine, histogram, 
                               fastPeriod, slowPeriod, signalPeriod);
            
        } catch (Exception e) {
            logger.error("Error calculating MACD", e);
            return null;
        }
    }
    
    public MACDData calculateCurrentMACD(List<CandleData> candles) {
        if (candles == null || candles.isEmpty()) {
            return null;
        }
        
        return calculateMACD(candles, candles.size() - 1);
    }
    
    private double calculateEMA(List<CandleData> candles, int index, int period) {
        // Start with SMA for the first value
        if (index < period - 1) {
            return calculateSMA(candles, index, period);
        }
        
        // Calculate EMA: EMA = (Close - Previous EMA) * Multiplier + Previous EMA
        double multiplier = 2.0 / (period + 1);
        double previousEMA = calculateEMA(candles, index - 1, period);
        double currentClose = candles.get(index).getClose();
        
        return (currentClose - previousEMA) * multiplier + previousEMA;
    }
    
    private double calculateSMA(List<CandleData> candles, int endIndex, int period) {
        int startIndex = Math.max(0, endIndex - period + 1);
        double sum = 0;
        int count = 0;
        
        for (int i = startIndex; i <= endIndex && i < candles.size(); i++) {
            sum += candles.get(i).getClose();
            count++;
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    private double calculateSignalLine(List<CandleData> candles, int index, double currentMACD) {
        // Simplified signal line calculation
        // In production, maintain a list of MACD values
        
        if (index < signalPeriod - 1) {
            return currentMACD;
        }
        
        // Calculate average of recent MACD values
        double sum = 0;
        int count = 0;
        
        for (int i = 0; i < signalPeriod && (index - i) >= 0; i++) {
            MACDData prevMACD = calculateMACD(candles, index - i);
            if (prevMACD != null) {
                sum += prevMACD.getMacdLine();
                count++;
            }
        }
        
        return count > 0 ? sum / count : currentMACD;
    }
    
    public boolean isBullishCrossover(MACDData previousMACD, MACDData currentMACD) {
        if (previousMACD == null || currentMACD == null) {
            return false;
        }
        
        return previousMACD.isBearish() && currentMACD.isBullish();
    }
    
    public boolean isBearishCrossover(MACDData previousMACD, MACDData currentMACD) {
        if (previousMACD == null || currentMACD == null) {
            return false;
        }
        
        return previousMACD.isBullish() && currentMACD.isBearish();
    }
    
    public boolean isHistogramRising(MACDData macd) {
        return macd != null && macd.isHistogramRising();
    }
    
    public boolean isHistogramFalling(MACDData macd) {
        return macd != null && macd.isHistogramFalling();
    }
    
    public double getCrossoverStrength(MACDData macd) {
        if (macd == null) return 0;
        return Math.abs(macd.getCrossoverValue());
    }
}