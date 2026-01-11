package main.java.com.mt5trading.core.services;

import com.mt5trading.models.CandleData;
import com.mt5trading.mt5.connector.MT5Connector;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MT5DataFetcher {
    private MT5Connector mt5Connector;
    private String symbol;
    private String timeframe;
    private boolean testMode = false;
    private List<CandleData> testData;
    
    public MT5DataFetcher(String symbol, String timeframe) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.mt5Connector = new MT5Connector();
    }
    
    // Enable test mode with mock data
    public void enableTestMode(List<CandleData> testData) {
        this.testMode = true;
        this.testData = testData;
        System.out.println("Test mode enabled with " + testData.size() + " candles");
    }
    
    public void disableTestMode() {
        this.testMode = false;
        this.testData = null;
    }
    
    public List<CandleData> fetchCandles(int count) {
        if (testMode && testData != null) {
            // Return test data
            int start = Math.max(0, testData.size() - count);
            List<CandleData> result = new ArrayList<>();
            for (int i = start; i < testData.size(); i++) {
                result.add(testData.get(i));
            }
            return result;
        }
        
        // Original MT5 connection code
        try {
            if (!mt5Connector.isConnected()) {
                if (!mt5Connector.connect()) {
                    System.err.println("Failed to connect to MT5");
                    return new ArrayList<>();
                }
            }
            return mt5Connector.getCandles(symbol, timeframe, count);
        } catch (Exception e) {
            System.err.println("Error fetching candles: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public CandleData getPreviousCandle() {
        List<CandleData> candles = fetchCandles(2);
        return candles.size() >= 2 ? candles.get(1) : null;
    }
    
    public CandleData getCurrentCandle() {
        List<CandleData> candles = fetchCandles(1);
        return candles.isEmpty() ? null : candles.get(0);
    }
    
    public boolean isNewCandleOpen(CandleData previousCandle) {
        if (previousCandle == null) return false;
        
        CandleData current = getCurrentCandle();
        return current != null && 
               current.getTime().isAfter(previousCandle.getTime());
    }
    
    // Helper method to fetch candles for specific time range
    public List<CandleData> fetchRecentCandles(int minutesBack) {
        // Simplified version - in real implementation, you'd query MT5 for specific time range
        int candlesNeeded = minutesBack / getTimeframeMinutes();
        return fetchCandles(Math.max(candlesNeeded, 10));
    }
    
    private int getTimeframeMinutes() {
        switch (timeframe) {
            case "M1": return 1;
            case "M5": return 5;
            case "M15": return 15;
            case "M30": return 30;
            case "H1": return 60;
            case "H4": return 240;
            case "D1": return 1440;
            default: return 15; // Default to M15
        }
    }
    
    public boolean isConnected() {
        return testMode || mt5Connector.isConnected();
    }
    
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public boolean isTestMode() { return testMode; }
}
