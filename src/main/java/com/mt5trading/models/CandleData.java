package com.mt5trading.models;

import java.time.LocalDateTime;

public class CandleData {
    private LocalDateTime time;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    
    public CandleData() {}
    
    public CandleData(LocalDateTime time, double open, double high, double low, double close, long volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    // Getters and Setters
    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    
    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }
    
    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }
    
    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }
    
    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }
    
    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }
    
    // Helper methods
    public boolean isBullish() {
        return close > open;
    }
    
    public boolean isBearish() {
        return close < open;
    }
    
    public double getBodySize() {
        return Math.abs(close - open);
    }
    
    public double getTotalRange() {
        return high - low;
    }
    
    public double getUpperWick() {
        return isBullish() ? high - close : high - open;
    }
    
    public double getLowerWick() {
        return isBullish() ? open - low : close - low;
    }
    
    public double getBodyMidpoint() {
        return (open + close) / 2.0;
    }
    
    @Override
    public String toString() {
        return String.format("Candle[Time: %s, O: %.5f, H: %.5f, L: %.5f, C: %.5f, V: %d, Bullish: %b]",
                time, open, high, low, close, volume, isBullish());
    }
}