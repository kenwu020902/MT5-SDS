package main.java.com.mt5trading.core.models;

import java.time.LocalDateTime;

public class CandleData {
    private LocalDateTime timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    
    // Constructors, getters, setters
    public boolean isBullish() {
        return close > open;
    }
    
    public boolean isBearish() {
        return close < open;
    }
    
    public double getBodyMidpoint() {
        return (open + close) / 2.0;
    }
}
