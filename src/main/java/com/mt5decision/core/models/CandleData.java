package main.java.com.mt5decision.core.models;

import java.time.LocalDateTime;

public class CandleData {
    private LocalDateTime time;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    
    public CandleData(double open, double high, double low, double close, long volume, LocalDateTime time) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.time = time;
    }
    
    public boolean isBullish() {
        return close > open;
    }
    
    public boolean isBearish() {
        return close < open;
    }
    
    public double getBodyMidpoint() {
        return (open + close) / 2.0;
    }
    
    // Getters and setters
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
    public LocalDateTime getTime() { return time; }
}