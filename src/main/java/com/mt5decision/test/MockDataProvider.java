package main.java.com.mt5decision.test;

import com.mt5decision.core.models.CandleData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockDataProvider {
    
    public static List<CandleData> getUS30Candles(int count, String direction) {
        List<CandleData> candles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(count * 15);
        
        double basePrice = 35000.0;
        
        for (int i = 0; i < count; i++) {
            double trendFactor = 0;
            if ("UPTREND".equals(direction)) {
                trendFactor = i * 10.0; // Upward trend
            } else if ("DOWNTREND".equals(direction)) {
                trendFactor = -i * 10.0; // Downward trend
            }
            
            double open = basePrice + trendFactor + (Math.random() * 50 - 25);
            double close = open + (Math.random() * 100 - 50);
            
            // Ensure bullish/bearish candles based on trend
            if ("UPTREND".equals(direction) && i > 0) {
                close = Math.max(close, open + 5); // Mostly bullish
            } else if ("DOWNTREND".equals(direction) && i > 0) {
                close = Math.min(close, open - 5); // Mostly bearish
            }
            
            double high = Math.max(open, close) + Math.random() * 30;
            double low = Math.min(open, close) - Math.random() * 30;
            long volume = (long)(1000 + Math.random() * 1000);
            
            candles.add(new CandleData(open, high, low, close, volume, 
                                      baseTime.plusMinutes(i * 15)));
        }
        
        return candles;
    }
    
    public static CandleData createBullishCandle() {
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        return new CandleData(100.0, 110.0, 95.0, 105.0, 1000, time);
    }
    
    public static CandleData createBearishCandle() {
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        return new CandleData(105.0, 108.0, 95.0, 98.0, 1000, time);
    }
    
    public static List<CandleData> getSampleCandles() {
        List<CandleData> candles = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(150);
        
        // Create a clear uptrend pattern
        candles.add(new CandleData(34900, 34950, 34880, 34930, 1200, baseTime.plusMinutes(0)));
        candles.add(new CandleData(34930, 34980, 34910, 34960, 1300, baseTime.plusMinutes(15)));
        candles.add(new CandleData(34960, 35010, 34940, 34990, 1400, baseTime.plusMinutes(30)));
        candles.add(new CandleData(34990, 35040, 34970, 35020, 1500, baseTime.plusMinutes(45)));
        candles.add(new CandleData(35020, 35070, 35000, 35050, 1600, baseTime.plusMinutes(60)));
        
        return candles;
    }
}
