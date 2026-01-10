package main.java.com.mt5decision.core.services;

import com.mt5decision.core.models.*;

public class TrendAnalyzer {
    private boolean useStrictConfirmation;
    private boolean useMACDConfirmation;
    
    public TrendAnalyzer(boolean useStrictConfirmation, boolean useMACDConfirmation) {
        this.useStrictConfirmation = useStrictConfirmation;
        this.useMACDConfirmation = useMACDConfirmation;
    }
    
    public TrendDirection confirmTrend(CandleData previousCandle, CandleData currentCandle, MACDData macdData) {
        // Bullish candle confirmation
        if (previousCandle.isBullish()) {
            boolean candleOK = useStrictConfirmation ? 
                (currentCandle.getOpen() > previousCandle.getHigh()) :
                (currentCandle.getOpen() > previousCandle.getClose());
                
            boolean macdOK = !useMACDConfirmation || 
                (macdData != null && macdData.isBullish());
            
            if (candleOK && macdOK) {
                return TrendDirection.UPTREND;
            }
        }
        
        // Bearish candle confirmation
        if (previousCandle.isBearish()) {
            boolean candleOK = useStrictConfirmation ?
                (currentCandle.getOpen() < previousCandle.getLow()) :
                (currentCandle.getOpen() < previousCandle.getClose());
                
            boolean macdOK = !useMACDConfirmation ||
                (macdData != null && macdData.isBearish());
            
            if (candleOK && macdOK) {
                return TrendDirection.DOWNTREND;
            }
        }
        
        return TrendDirection.NONE;
    }
    
    // Enhanced trend confirmation with market structure
    public TrendDirection confirmTrendWithStructure(
            CandleData previousCandle, 
            CandleData currentCandle,
            List<CandleData> recentCandles,
            MACDData macdData) {
        
        TrendDirection basicTrend = confirmTrend(previousCandle, currentCandle, macdData);
        
        if (basicTrend == TrendDirection.NONE) {
            return TrendDirection.NONE;
        }
        
        // Check market structure for higher highs/lows or lower highs/lows
        boolean structureValid = false;
        
        if (basicTrend == TrendDirection.UPTREND) {
            structureValid = hasHigherHighsAndLows(recentCandles);
        } else if (basicTrend == TrendDirection.DOWNTREND) {
            structureValid = hasLowerHighsAndLows(recentCandles);
        }
        
        return structureValid ? basicTrend : TrendDirection.NONE;
    }
    
    private boolean hasHigherHighsAndLows(List<CandleData> candles) {
        // Implement higher highs and higher lows logic
        if (candles.size() < 3) return false;
        
        for (int i = 1; i < candles.size() - 1; i++) {
            if (candles.get(i).getHigh() <= candles.get(i-1).getHigh() ||
                candles.get(i).getLow() <= candles.get(i-1).getLow()) {
                return false;
            }
        }
        return true;
    }
    
    private boolean hasLowerHighsAndLows(List<CandleData> candles) {
        // Implement lower highs and lower lows logic
        if (candles.size() < 3) return false;
        
        for (int i = 1; i < candles.size() - 1; i++) {
            if (candles.get(i).getHigh() >= candles.get(i-1).getHigh() ||
                candles.get(i).getLow() >= candles.get(i-1).getLow()) {
                return false;
            }
        }
        return true;
    }
}