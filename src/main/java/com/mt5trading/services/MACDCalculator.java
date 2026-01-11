package main.java.com.mt5trading.core.services;

import com.mt5trading.models.CandleData;
import com.mt5trading.models.MACDData;
import java.util.List;
import java.util.ArrayList;

public class MACDCalculator {
    private static final int FAST_PERIOD = 12;
    private static final int SLOW_PERIOD = 26;
    private static final int SIGNAL_PERIOD = 9;
    
    public MACDData calculateMACD(List<CandleData> candles) {
        if (candles == null || candles.size() < SLOW_PERIOD) {
            return null;
        }
        
        List<Double> closes = new ArrayList<>();
        for (CandleData candle : candles) {
            closes.add(candle.getClose());
        }
        
        // Calculate EMAs
        List<Double> fastEMA = calculateEMA(closes, FAST_PERIOD);
        List<Double> slowEMA = calculateEMA(closes, SLOW_PERIOD);
        
        // Calculate MACD line
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < fastEMA.size(); i++) {
            if (i < slowEMA.size()) {
                macdLine.add(fastEMA.get(i) - slowEMA.get(i));
            }
        }
        
        // Calculate Signal line (EMA of MACD line)
        List<Double> signalLine = calculateEMA(macdLine, SIGNAL_PERIOD);
        
        // Calculate Histogram
        List<Double> histogram = new ArrayList<>();
        for (int i = 0; i < signalLine.size(); i++) {
            if (i < macdLine.size()) {
                histogram.add(macdLine.get(i) - signalLine.get(i));
            }
        }
        
        // Get the latest values (index 0 is latest)
        if (!macdLine.isEmpty() && !signalLine.isEmpty() && !histogram.isEmpty()) {
            double latestMacd = macdLine.get(0);
            double latestSignal = signalLine.get(0);
            double latestHistogram = histogram.get(0);
            
            // Get previous values for comparison
            double previousMacd = macdLine.size() > 1 ? macdLine.get(1) : latestMacd;
            double previousSignal = signalLine.size() > 1 ? signalLine.get(1) : latestSignal;
            double previousHistogram = histogram.size() > 1 ? histogram.get(1) : latestHistogram;
            
            return new MACDData(latestMacd, latestSignal, latestHistogram, 
                               previousMacd, previousSignal, previousHistogram);
        }
        
        return null;
    }
    
    private List<Double> calculateEMA(List<Double> values, int period) {
        List<Double> ema = new ArrayList<>();
        if (values.isEmpty() || period <= 0) {
            return ema;
        }
        
        // Calculate SMA for first value
        double sma = 0;
        int startIndex = Math.max(0, values.size() - period);
        for (int i = startIndex; i < values.size(); i++) {
            sma += values.get(i);
        }
        sma /= Math.min(period, values.size() - startIndex);
        ema.add(sma);
        
        // Calculate multiplier
        double multiplier = 2.0 / (period + 1);
        
        // Calculate EMA for remaining values
        for (int i = values.size() - 2; i >= 0; i--) {
            double currentEMA = (values.get(i) - ema.get(ema.size() - 1)) * multiplier + ema.get(ema.size() - 1);
            ema.add(currentEMA);
        }
        
        // Reverse to have latest first
        List<Double> reversed = new ArrayList<>();
        for (int i = ema.size() - 1; i >= 0; i--) {
            reversed.add(ema.get(i));
        }
        
        return reversed;
    }
    
    public MACDData calculateMACDForSingleCandle(List<CandleData> candles, int index) {
        if (candles == null || index < 0 || index >= candles.size()) {
            return null;
        }
        
        // Get a subset of candles for calculation
        int start = Math.max(0, index - SLOW_PERIOD);
        int end = index + 1;
        List<CandleData> subset = candles.subList(start, end);
        
        return calculateMACD(subset);
    }
    
    // Helper method to check if MACD is showing divergence
    public boolean hasBullishDivergence(List<CandleData> candles, List<MACDData> macdValues) {
        if (candles.size() < 5 || macdValues.size() < 5) {
            return false;
        }
        
        // Check for lower lows in price but higher lows in MACD
        int lastIndex = Math.min(candles.size(), macdValues.size()) - 1;
        
        // Find the last two significant lows
        Double priceLow1 = null, priceLow2 = null;
        Double macdLow1 = null, macdLow2 = null;
        
        for (int i = lastIndex; i >= 0; i--) {
            if (i < lastIndex && candles.get(i).getLow() < candles.get(i+1).getLow()) {
                if (priceLow1 == null) {
                    priceLow1 = candles.get(i).getLow();
                    macdLow1 = macdValues.get(i).getMacdLine();
                } else if (priceLow2 == null) {
                    priceLow2 = candles.get(i).getLow();
                    macdLow2 = macdValues.get(i).getMacdLine();
                    break;
                }
            }
        }
        
        if (priceLow1 != null && priceLow2 != null && macdLow1 != null && macdLow2 != null) {
            return priceLow2 < priceLow1 && macdLow2 > macdLow1;
        }
        
        return false;
    }
    
    public boolean hasBearishDivergence(List<CandleData> candles, List<MACDData> macdValues) {
        if (candles.size() < 5 || macdValues.size() < 5) {
            return false;
        }
        
        // Check for higher highs in price but lower highs in MACD
        int lastIndex = Math.min(candles.size(), macdValues.size()) - 1;
        
        Double priceHigh1 = null, priceHigh2 = null;
        Double macdHigh1 = null, macdHigh2 = null;
        
        for (int i = lastIndex; i >= 0; i--) {
            if (i < lastIndex && candles.get(i).getHigh() > candles.get(i+1).getHigh()) {
                if (priceHigh1 == null) {
                    priceHigh1 = candles.get(i).getHigh();
                    macdHigh1 = macdValues.get(i).getMacdLine();
                } else if (priceHigh2 == null) {
                    priceHigh2 = candles.get(i).getHigh();
                    macdHigh2 = macdValues.get(i).getMacdLine();
                    break;
                }
            }
        }
        
        if (priceHigh1 != null && priceHigh2 != null && macdHigh1 != null && macdHigh2 != null) {
            return priceHigh2 > priceHigh1 && macdHigh2 < macdHigh1;
        }
        
        return false;
    }
}
