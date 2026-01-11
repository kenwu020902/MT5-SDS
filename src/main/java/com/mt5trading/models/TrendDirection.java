package com.mt5trading.models;

public enum TrendDirection {
    NONE("None"),
    UPTREND("Uptrend"),
    DOWNTREND("Downtrend");
    
    private final String description;
    
    TrendDirection(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isBullish() {
        return this == UPTREND;
    }
    
    public boolean isBearish() {
        return this == DOWNTREND;
    }
    
    public static TrendDirection fromString(String text) {
        for (TrendDirection dir : TrendDirection.values()) {
            if (dir.name().equalsIgnoreCase(text)) {
                return dir;
            }
        }
        return NONE;
    }
}